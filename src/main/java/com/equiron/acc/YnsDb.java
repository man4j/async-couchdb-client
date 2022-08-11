package com.equiron.acc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.equiron.acc.annotation.YnsDbConfig;
import com.equiron.acc.annotation.YnsErlangView;
import com.equiron.acc.annotation.YnsJsView;
import com.equiron.acc.annotation.YnsReplicated;
import com.equiron.acc.annotation.YnsReplicated.Direction;
import com.equiron.acc.annotation.YnsSecurity;
import com.equiron.acc.annotation.YnsSecurityPattern;
import com.equiron.acc.annotation.YnsValidateDocUpdate;
import com.equiron.acc.annotation.model.AnnotationConfigOption;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDbInfo;
import com.equiron.acc.json.YnsDbInfo.YnsClusterInfo;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsInstanceInfo;
import com.equiron.acc.json.YnsReplicationDocument;
import com.equiron.acc.json.security.YnsSecurityObject;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderType;
import com.equiron.acc.provider.JdkHttpClientProvider;
import com.equiron.acc.provider.OkHttpClientProvider;
import com.equiron.acc.util.NamedStrategy;
import com.equiron.acc.util.ReflectionUtils;
import com.equiron.acc.util.StreamResponse;
import com.equiron.acc.util.UrlBuilder;
import com.equiron.acc.view.YnsBuiltInView;
import com.equiron.acc.view.YnsMapReduceView;
import com.equiron.acc.view.YnsMapView;
import com.equiron.acc.view.YnsReduceView;
import com.equiron.acc.view.YnsView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YnsDb implements AutoCloseable {
    private volatile com.equiron.acc.YnsDbConfig config;
    
    @Autowired
    private ApplicationContext ctx;

    final ObjectMapper mapper = new ObjectMapper();

    private volatile String host;
    
    private volatile int port;
    
    private volatile String user;
    
    private volatile String password;
    
    private volatile String dbName;
    
    private volatile int clientMaxParallelism;
    
    private volatile HttpClientProviderType httpClientProviderType;
    
    private volatile YnsBuiltInView builtInView;

    private volatile YnsDocumentOperations operations;
    
    private volatile YnsAdminOperations adminOperations;
    
    private volatile boolean selfDiscovering = true;
    
    private volatile boolean buildViewOnStart = true;
    
    private boolean removeNotDeclaredReplications = false;
    
    private volatile List<YnsView> viewList = new CopyOnWriteArrayList<>();
    
    private volatile YnsClusterInfo clusterInfo;
    
    private volatile HttpClientProvider httpClientProvider;

    public YnsDb(com.equiron.acc.YnsDbConfig config) {
        this.config = config;

        mapper.registerModule(new JavaTimeModule());
        
        applyConfig();
        
        httpClientProvider = httpClientProviderType == HttpClientProviderType.JDK ? new JdkHttpClientProvider() : new OkHttpClientProvider();
        
        if (user != null && password != null) {
            httpClientProvider.setCredentials(user, password);
        }
                                                              
        operations = new YnsDocumentOperations(this);
        
        builtInView = new YnsBuiltInView(this);
        
        viewList.add(builtInView);
        
        testConnection();

        createDbIfNotExist();
        
        if (selfDiscovering) {            
            if (!getDbName().equals("_users")) {//тк данная база содержит встроенный дизайн-документ
                synchronizeDesignDocs();
            }

            injectViews();

            injectValidators();

            addSecurity();

            cleanupViews();
            
            synchronizeReplicationDocs();
        }
    }
    
    public com.equiron.acc.YnsDbConfig getConfig() {
        return config;
    }
    
    public HttpClientProvider getHttpClientProvider() {
        return httpClientProvider;
    }
    
    public ObjectMapper getMapper() {
        return mapper;
    }

    public String getDbName() {
        return dbName;
    }
    
    public int getClientMaxParallelism() {
        return clientMaxParallelism;
    }
    
    public String getServerUrl() {
        return String.format("http://%s:%s", host, port);
    }

    public String getDbUrl() {
        return new UrlBuilder(getServerUrl()).addPathSegment(getDbName()).toString();
    }
    
    public YnsClusterInfo getClusterInfo() {
        return clusterInfo;
    }
    
    public YnsBuiltInView getBuiltInView() {
        return builtInView;
    }
    
    public YnsDocumentOperations getOperations() {
        return operations;
    }

    public void updateAllViews() {
        for (YnsView view : viewList) {
            updateView(view, false);
        }
    }

    //------------------ Fetch API -------------------------
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends YnsDocument> T get(String docId) {
        return operations.get(docId);
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId) {
        return operations.getRaw(docId);
    }
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends YnsDocument> T get(String docId, boolean attachments) {
        return operations.get(docId, attachments);
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        return operations.getRaw(docId, attachments);
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends YnsDocument> List<T> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        return operations.saveOrUpdate(doc, docs);
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends YnsDocument> List<T> saveOrUpdate(List<T> docs) {
        return operations.saveOrUpdate(docs);
    }
    
    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends YnsDocument> List<T> saveOrUpdate(List<T> docs, boolean ignoreConflicts) {
        return operations.saveOrUpdate(docs, ignoreConflicts);
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    @SafeVarargs
    public final List<YnsBulkResponse> saveOrUpdate(Map<String, Object>... docs) {
        return operations.saveOrUpdate(docs);
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<YnsBulkResponse> delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) {
        return operations.delete(docRev, docRevs);
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<YnsBulkResponse> delete(List<YnsDocIdAndRev> docRevs) {
        return operations.delete(docRevs);
    }
    
    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        return operations.attach(docIdAndRev, in, name, contentType);
    }

    /**
     * Attach content to non-existing document.
     */
    public YnsBulkResponse attach(String docId, InputStream in, String name, String contentType) {
        return operations.attach(new YnsDocIdAndRev(docId, null), in, name, contentType);
    }
    
    /**
     * Gets an attachment of the document as stream.
     */
    public StreamResponse getAttachmentAsStream(String docId, String name) {
        return operations.getAttachmentAsStream(docId, name);
    }
    
    /**
     * Gets an attachment of the document as stream.
     */
    public StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers) {
        return operations.getAttachmentAsStream(docId, name, headers);
    }

    /**
     * Deletes an attachment from the document.
     */
    public boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name) {
        return operations.deleteAttachment(docIdAndRev, name);
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return adminOperations.getDesignDocs();
    }
    
    public List<CouchDbDesignDocument> getDesignDocsWithValidators() {
        return adminOperations.getDesignDocsWithValidators();
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public List<String> getDatabases() {
        return adminOperations.getDatabases();
    }

    /**
     * Create a new database.
     */
    public boolean createDb() {
        return adminOperations.createDb();
    }

    /**
     * Delete an existing database.
     */
    public boolean deleteDb() {
        return adminOperations.deleteDb();
    }

    /**
     * Returns database information.
     */
    public YnsDbInfo getInfo() {
        return adminOperations.getInfo();
    }
    
    /**
     * Accessing the root of a CouchDB instance returns meta information about the instance. 
     * The response is a JSON structure containing information about the server, including a 
     * welcome message and the version of the server.     
     */
    public YnsInstanceInfo getInstanceInfo() {
        return adminOperations.getInstanceInfo();
    }

    /**
     * Removes view files that are not used by any design document, requires admin privileges.
     *
     * View indexes on disk are named after their MD5 hash of the view definition.
     * When you change a view, old indexes remain on disk. To clean up all outdated view indexes
     * (files named after the MD5 representation of views, that does not exist anymore) you can
     * trigger a view cleanup.
     */
    public boolean cleanupViews() {
        return adminOperations.cleanupViews();
    }

    //------------------ Discovering methods -------------------------

    private void testConnection() {
        while (true) {
            try {
                if (httpClientProvider.get(getServerUrl()).getStatus() != 200) {
                    throw new IOException("Could not connect to " + getServerUrl());
                }
                
                break;
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
                log.warn("Waiting for database...");
                
                try {
                    Thread.sleep(1000);
                } catch (@SuppressWarnings("unused") Exception e1) {
                    System.exit(1);
                }
            } catch (Exception e) {
                log.error("", e);
                
                System.exit(1);
            }
        }
    }

    private void applyConfig() {
        String _host = config.getHost();
        String _port = config.getPort() + "";
        String _user = config.getUser();
        String _password = config.getPassword();
        String _dbName = config.getDbName() == null ? NamedStrategy.addUnderscores(getClass().getSimpleName()) : config.getDbName();
        String _clientMaxParallelism = config.getClientMaxParallelism() + "";
        buildViewOnStart = config.isBuildViewsOnStart();
        selfDiscovering = config.isSelfDiscovering();
        removeNotDeclaredReplications = config.isRemoveNotDeclaredReplications();
        httpClientProviderType = config.getHttpClientProviderType();
        
        if (getClass().isAnnotationPresent(com.equiron.acc.annotation.YnsDbConfig.class)) {
            YnsDbConfig annotationConfig = getClass().getAnnotation(YnsDbConfig.class);

            if (!annotationConfig.host().isBlank()) _host = annotationConfig.host();
            if (!annotationConfig.port().isBlank()) _port = annotationConfig.port();
            if (!annotationConfig.user().isBlank()) _user = annotationConfig.user();
            if (!annotationConfig.password().isBlank()) _password = annotationConfig.password();
            if (!annotationConfig.dbName().isBlank()) _dbName = annotationConfig.dbName();
            if (!annotationConfig.clientMaxParallelism().isBlank()) _clientMaxParallelism = annotationConfig.clientMaxParallelism();
            if (annotationConfig.buildViewOnStart() != AnnotationConfigOption.BY_CONFIG) buildViewOnStart = annotationConfig.buildViewOnStart() == AnnotationConfigOption.ENABLED ? true : false;
            if (annotationConfig.selfDiscovering() != AnnotationConfigOption.BY_CONFIG) selfDiscovering = annotationConfig.selfDiscovering() == AnnotationConfigOption.ENABLED ? true : false;
            if (annotationConfig.removeNotDeclaredReplications() != AnnotationConfigOption.BY_CONFIG) removeNotDeclaredReplications = annotationConfig.removeNotDeclaredReplications() == AnnotationConfigOption.ENABLED ? true : false;
        }
        
        this.host = resolve(_host);
        this.port = Integer.parseInt(resolve(_port));
        this.user = resolve(_user);
        this.password = resolve(_password);
        this.dbName = resolve(_dbName);
        this.clientMaxParallelism = Integer.parseInt(resolve(_clientMaxParallelism));
    }

    private void createDbIfNotExist() {
        if (!getDatabases().contains(getDbName())) {
            createDb();
        }
        
        clusterInfo = getInfo().getCluster();
    }

    private void injectViews() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            String viewName = NamedStrategy.addUnderscores(field.getName());
            String designName = viewName;

            Class<?> viewClass = field.getType();

            TypeFactory tf = TypeFactory.defaultInstance();

            JavaType[] jts = tf.findTypeParameters(tf.constructType(field.getGenericType()), viewClass);

            YnsView injectedView = null;

            if (viewClass == YnsMapView.class) {
                injectedView = new YnsMapView<>(this, designName, viewName, jts);
            }

            if (viewClass == YnsReduceView.class) {
                injectedView = new YnsReduceView<>(this, designName, viewName, jts);
            }

            if (viewClass == YnsMapReduceView.class) {
                injectedView = new YnsMapReduceView<>(this, designName, viewName, jts);
            }

            if (injectedView != null) {
                if (buildViewOnStart) {
                    updateView(injectedView, true);
                }
                
                setValue(field, injectedView);
                viewList.add(injectedView);
            }
        }
    }

    private void updateView(YnsView view, boolean debugEnable) {
        long t = System.currentTimeMillis();
        
        if (debugEnable) {
            log.info("Updating view in database: " + getDbName() + ". View: " + view.getDesignName() + "/" + view.getViewName() + "...");
        }
        
        boolean complete = false;
        
        while (!complete) {
            try {
                view.update();
                complete = true;
            } catch (Exception e) {
                if (e instanceof IOException) {
                    log.warn("Not critical exception: " + e.getMessage());
                }
            }
        }
        
        if (debugEnable) {
            log.info("Complete updating view in database: " + getDbName() + ". View: " + view.getDesignName() + "/" + view.getViewName()  + " in " + (System.currentTimeMillis() - t) + " ms");
        }
    }

    private void injectValidators() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(YnsValidateDocUpdate.class)) {
                YnsValidateDocUpdate vdu = field.getAnnotation(YnsValidateDocUpdate.class);

                setValue(field, new YnsValidator(vdu.value()));
            }
        }
    }
        
    private void addSecurity() {
        try {
            YnsSecurityObject oldSecurityObject = getSecurityObject();
            
            if (getClass().isAnnotationPresent(YnsSecurity.class)) {
                YnsSecurityPattern adminsPattern = getClass().getAnnotation(YnsSecurity.class).admins();
                YnsSecurityPattern membersPattern = getClass().getAnnotation(YnsSecurity.class).members();
                
                YnsSecurityObject securityObject = new YnsSecurityObject();
                
                securityObject.setAdmins(new com.equiron.acc.json.security.YnsSecurityPattern(new HashSet<>(Arrays.asList(adminsPattern.names())), new HashSet<>(Arrays.asList(adminsPattern.roles()))));
                securityObject.setMembers(new com.equiron.acc.json.security.YnsSecurityPattern(new HashSet<>(Arrays.asList(membersPattern.names())), new HashSet<>(Arrays.asList(membersPattern.roles()))));
                
                if (!oldSecurityObject.equals(securityObject)) {
                    putSecurityObject(securityObject);
                }
            } else {
                YnsSecurityObject defaultSecurityObject = new YnsSecurityObject();
                
                if (!oldSecurityObject.equals(defaultSecurityObject)) {
                    putSecurityObject(defaultSecurityObject);//clean security object
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void synchronizeReplicationDocs() {
        Map<String, YnsReplicationDocument> newReplicationDocs = new HashMap<>();

        for (YnsReplicated replicated : getClass().getAnnotationsByType(YnsReplicated.class)) {
            String _enabled = resolve(replicated.enabled());
            
            if (!_enabled.isBlank() && _enabled.equalsIgnoreCase("true")) {
                String protocol = resolve(replicated.targetProtocol());
                String _host = resolve(replicated.targetHost());
                String _port = resolve(replicated.targetPort());
                String _user = resolve(replicated.targetUser());
                String _password = resolve(replicated.targetPassword());
                String remoteDb = resolve(replicated.targetDbName());
                String selector = resolve(replicated.selector());
                boolean createTarget = resolve(replicated.createTarget()).equalsIgnoreCase("true");
                
                if (remoteDb.isBlank()) {
                    remoteDb = getDbName();
                }
                
                String remoteServer;
                String remoteServerWithoutCreds;
                
                if (_user.isBlank() && _password.isBlank()) {
                    remoteServer = String.format("%s://%s:%s/%s", protocol, _host, _port, remoteDb);
                    remoteServerWithoutCreds = String.format("%s://%s:%s/%s", protocol, _user, _password, _host, _port, remoteDb);
                } else {
                    remoteServer = String.format("%s://%s:%s@%s:%s/%s", protocol, _user, _password, _host, _port, remoteDb);
                    remoteServerWithoutCreds = String.format("%s://***:***@%s:%s/%s", protocol, _host, _port, remoteDb);
                }
                
                String localServer;
                String localServerWithoutCreds;
                
                if (this.user == null && this.password == null) {
                    localServer = String.format("http://%s:%s/%s", this.host, this.port, getDbName());
                    localServerWithoutCreds = String.format("http://%s:%s/%s", this.user, this.password, this.host, this.port, getDbName());
                } else {
                    localServer = String.format("http://%s:%s@%s:%s/%s", this.user, this.password, this.host, this.port, getDbName());
                    localServerWithoutCreds = String.format("http://***:***@%s:%s/%s", this.host, this.port, getDbName());
                }
                
                Map<String, Object> selectorMap = null;
                
                if (!selector.isBlank()) {
                    try {
                        selectorMap = mapper.readValue(selector, new TypeReference<Map<String, Object>>(){/*empty*/});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                if (replicated.direction() == Direction.TO || replicated.direction() == Direction.BOTH) { 
                    YnsReplicationDocument toRemote = new YnsReplicationDocument(getDbName() + ">>>" + protocol + "://" + _host + ":" + _port + "/" + remoteDb, 
                                                                                 localServer, 
                                                                                 remoteServer, 
                                                                                 selectorMap,
                                                                                 createTarget);
                    newReplicationDocs.put(toRemote.getDocId(), toRemote);
                }
                
                if (replicated.direction() == Direction.FROM || replicated.direction() == Direction.BOTH) {
                    YnsReplicationDocument fromRemote = new YnsReplicationDocument(getDbName() + "<<<" + protocol + "://" + _host + ":" + _port + "/" + remoteDb, 
                                                                                   remoteServer, 
                                                                                   localServer, 
                                                                                   selectorMap,
                                                                                   createTarget);
                    newReplicationDocs.put(fromRemote.getDocId(), fromRemote);
                }
                
                switch (replicated.direction()) {
                    case TO:   log.info("Prepare one way replication: {} -> {} ",   localServerWithoutCreds,  remoteServerWithoutCreds); break;
                    case FROM: log.info("Prepare one way replication: {} -> {} ",   remoteServerWithoutCreds, localServerWithoutCreds);  break;
                    case BOTH: log.info("Prepare two ways replication: {} <-> {} ", localServerWithoutCreds,  remoteServerWithoutCreds); break;
                    default: throw new IllegalStateException();
                }
            } else {
                log.warn("Replication for " + getDbName() + " disabled");
            }
        }
        
        com.equiron.acc.YnsDbConfig _config = new com.equiron.acc.YnsDbConfig.Builder().setHost(host)
                                                                                       .setPort(port)
                                                                                       .setUser(user)
                                                                                       .setPassword(password)
                                                                                       .build();
        
        try (ReplicatorDb replicatorDb = new ReplicatorDb(_config)) {        
            List<String> replicationsDocsIds = replicatorDb.getBuiltInView().createQuery()
                                                                            .asIds()
                                                                            .stream()
                                                                            .filter(id -> !id.startsWith("_design/"))//exclude design docs
                                                                            .toList();
            
            List<YnsReplicationDocument> replicationsDocs = new ArrayList<>();
            
            for (String docId : replicationsDocsIds) {
                try {
                    replicationsDocs.add(replicatorDb.get(docId));
                } catch (Exception e) {
                    log.warn("Can't parse replication document: " + e.getMessage());
                }
            }
            
            for (var doc : replicationsDocs) {
                if (doc.getDocId().startsWith(getDbName() + "<<<") || doc.getDocId().startsWith(getDbName() + ">>>")) {//находим документы связанные с этой базой
                    if (newReplicationDocs.containsKey(doc.getDocId())) {//значит надо просто обновить
                        YnsReplicationDocument updatedReplicationDoc = newReplicationDocs.get(doc.getDocId());
                        
                        if (!updatedReplicationDoc.equals(doc)) {
                            updatedReplicationDoc.setRev(doc.getRev());
                        
                            replicatorDb.saveOrUpdate(updatedReplicationDoc);
                     
                            if (!updatedReplicationDoc.isOk()) {
                                log.warn("Update replication failed: " + updatedReplicationDoc.getConflictReason());
                            }
                        }
                        
                        newReplicationDocs.remove(doc.getDocId());
                    } else if (removeNotDeclaredReplications) {
                        replicatorDb.delete(doc.getDocIdAndRev());
                    }
                }
            }
            
            if (!newReplicationDocs.isEmpty()) {
                replicatorDb.saveOrUpdate(new ArrayList<>(newReplicationDocs.values()));
                
                for (var d : newReplicationDocs.values()) {
                    if (!d.isOk()) {
                        log.warn("Add replication failed: " + d.getConflictReason());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolve(String param) {        
        if (param != null && ctx != null) {
            try {
                return (String) resolveExpression(param);
            } catch (@SuppressWarnings("unused") Exception e) {
                //empty
            }
        }
        
        return param;
    }
    
    private Object resolveExpression(String expression) {
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();

        String placeholdersResolved = bf.resolveEmbeddedValue(expression);
        BeanExpressionResolver expressionResolver = bf.getBeanExpressionResolver();
        
        return expressionResolver.evaluate(placeholdersResolved, new BeanExpressionContext(bf, null));
    }

    @SneakyThrows
    public void putSecurityObject(YnsSecurityObject securityObject) {
        if (httpClientProvider.put(new UrlBuilder(getDbUrl()).addPathSegment("_security").build(), mapper.writeValueAsString(securityObject)).getStatus() != 200) {
            throw new RuntimeException("Can't apply security");
        }
    }

    @SneakyThrows
    public YnsSecurityObject getSecurityObject() {
        return mapper.readValue(httpClientProvider.get(new UrlBuilder(getDbUrl()).addPathSegment("_security").build()).getBody(), YnsSecurityObject.class);
    }
    
    private void synchronizeDesignDocs() {
        Set<CouchDbDesignDocument> oldDesignDocs = new HashSet<>(getDesignDocsWithValidators());

        Set<CouchDbDesignDocument> newDesignDocs = generateNewDesignDocs();

        for (CouchDbDesignDocument oldDoc : oldDesignDocs) {
            if (!newDesignDocs.contains(oldDoc)) {
                delete(oldDoc.getDocIdAndRev());
            } else {
                newDesignDocs.remove(oldDoc);
            }
        }

        if (!newDesignDocs.isEmpty()) {
            saveOrUpdate(new ArrayList<>(newDesignDocs));
        }
    }

    private Set<CouchDbDesignDocument> generateNewDesignDocs() {
        Set<CouchDbDesignDocument> designSet = new HashSet<>();

        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(YnsJsView.class) || field.isAnnotationPresent(YnsErlangView.class)) {
                String viewName = NamedStrategy.addUnderscores(field.getName());
                String designName = "_design/" + viewName;
                
                String map = null;
                String reduce = null;
                CouchDbDesignDocument designDocument = null;

                if (field.isAnnotationPresent(YnsJsView.class)) {
                    YnsJsView view = field.getAnnotation(YnsJsView.class);

                    map = "function(doc) {" + view.map() + ";}";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(YnsJsView.COUNT, YnsJsView.STATS, YnsJsView.SUM).contains(view.reduce())) {
                            reduce = view.reduce();
                        } else {
                            reduce = "function(key, values, rereduce) {" + view.reduce() + ";}";
                        }
                    }
                    
                    designDocument = new CouchDbDesignDocument(designName);
                } else {
                    YnsErlangView view = field.getAnnotation(YnsErlangView.class);

                    map = "fun({Doc}) -> " + view.map() + " end.";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(YnsErlangView.COUNT, YnsErlangView.STATS, YnsErlangView.SUM).contains(view.reduce())) {
                            reduce = view.reduce();
                        } else {
                            reduce = "fun(Keys, Values, ReReduce) -> " + view.reduce() + " end.";
                        }
                    }
                    
                    designDocument = new CouchDbDesignDocument(designName, "erlang");
                }
                
                designDocument.addView(viewName, map, reduce);
                designSet.add(designDocument);
            }
            
            if (field.isAnnotationPresent(YnsValidateDocUpdate.class)) {
                field.setAccessible(true);

                YnsValidateDocUpdate vdu = field.getAnnotation(YnsValidateDocUpdate.class);

                String designName = "_design/" + NamedStrategy.addUnderscores(field.getName());
                
                CouchDbDesignDocument designDocument = new CouchDbDesignDocument(designName);
                designDocument.setValidateDocUpdate("function(newDoc, oldDoc, userCtx, secObj) { " + vdu.value() + ";}");
                
                designSet.add(designDocument);
            }
        }

        return designSet;
    }
    
    private void setValue(Field field, Object value) {
        field.setAccessible(true);

        try {
            field.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        //empty
    }
}