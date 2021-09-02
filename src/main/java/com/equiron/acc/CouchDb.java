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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.equiron.acc.annotation.ErlangView;
import com.equiron.acc.annotation.JsView;
import com.equiron.acc.annotation.Replicated;
import com.equiron.acc.annotation.Replicated.Direction;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.annotation.ValidateDocUpdate;
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbInfo;
import com.equiron.acc.json.CouchDbInfo.CouchDbClusterInfo;
import com.equiron.acc.json.CouchDbInstanceInfo;
import com.equiron.acc.json.CouchDbReplicationDocument;
import com.equiron.acc.json.security.CouchDbSecurityObject;
import com.equiron.acc.json.security.CouchDbSecurityPattern;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderType;
import com.equiron.acc.provider.JdkHttpClientProvider;
import com.equiron.acc.provider.OkHttpClientProvider;
import com.equiron.acc.util.NamedStrategy;
import com.equiron.acc.util.ReflectionUtils;
import com.equiron.acc.util.UrlBuilder;
import com.equiron.acc.view.CouchDbBuiltInView;
import com.equiron.acc.view.CouchDbMapReduceView;
import com.equiron.acc.view.CouchDbMapView;
import com.equiron.acc.view.CouchDbReduceView;
import com.equiron.acc.view.CouchDbView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rainerhahnekamp.sneakythrow.Sneaky;

public class CouchDb implements AutoCloseable {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private volatile CouchDbConfig config;
    
    @Autowired
    private ApplicationContext ctx;

    final ObjectMapper mapper = new ObjectMapper();

    private volatile String host;
    
    private volatile int port;
    
    private volatile String user;
    
    private volatile String password;
    
    private volatile String dbName;
    
    private volatile boolean enabled = true;
    
    private volatile HttpClientProviderType httpClientProviderType;
    
    private volatile CouchDbBuiltInView builtInView;

    private volatile CouchDbOperations operations;
    
    private volatile boolean selfDiscovering = true;
    
    private volatile boolean enablePurgeListener = false;
    
    private boolean leaveStaleReplications = false;
    
    private volatile boolean initialized;
    
    private volatile List<CouchDbView> viewList = new CopyOnWriteArrayList<>();
    
    private volatile CouchDbClusterInfo clusterInfo;
    
    private volatile CouchDbEventListener<CouchDbDocument> purgeListener = new CouchDbEventListener<>(this) {/* empty */};
    
    private volatile HttpClientProvider httpClientProvider;
    
    @SuppressWarnings("resource")
	@PostConstruct
    public void init() {
        if (!initialized) {//может быть инициализирована в конструкторе
            mapper.registerModule(new JavaTimeModule());
            
            applyConfig();
            
            if (enabled) {
                httpClientProvider = httpClientProviderType == HttpClientProviderType.JDK ? new JdkHttpClientProvider() : new OkHttpClientProvider();
                
                if (user != null && password != null) {
                    httpClientProvider.setCredentials(user, password);
                }
                                                                      
                operations = new CouchDbOperations(this);
                
                builtInView = new CouchDbBuiltInView(this);
                
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
                
                initialized = true;

                if (enablePurgeListener) {
                    purgeListener.addEventHandler(e -> {
                        if (e.isDeleted()) {    
                            purge(e.getDocIdAndRev());
                        }
                    });
                    
                    purgeListener.startListening("0");
                }
            } else {
                logger.warn("Database " + getDbName() + " was disabled!");
            }
        }
    }
    
    public CouchDb() {
        //empty
    }
    
    public CouchDb(CouchDbConfig config) {
        this.config = config;
        
        init();
    }
    
    public CouchDbOperations getOperations() {
        return operations;
    }

    public HttpClientProvider getHttpClientProvider() {
        return httpClientProvider;
    }
    
    @Autowired
    public void setConfig(CouchDbConfig config) {
        if (this.config == null) {//может быть уже инициализирован через конструктор
            this.config = config;
        }
    }
    
    public ObjectMapper getMapper() {
        return mapper;
    }

    public String getDbName() {
        return dbName;
    }
    
    public String getServerUrl() {
        return String.format("http://%s:%s", host, port);
    }

    public String getDbUrl() {
        return new UrlBuilder(getServerUrl()).addPathSegment(getDbName()).toString();
    }
    
    public CouchDbClusterInfo getClusterInfo() {
        return clusterInfo;
    }
    
    public CouchDbBuiltInView getBuiltInView() {
        return builtInView;
    }

    public void updateAllViews() {
        for (CouchDbView view : viewList) {
            updateView(view, false);
        }
    }

    //------------------ Fetch API -------------------------
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(String docId) {
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
    public <T extends CouchDbDocument> T get(String docId, boolean attachments) {
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
    public <T extends CouchDbDocument> List<T> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        return operations.saveOrUpdate(doc, docs);
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs) {
        return operations.saveOrUpdate(docs);
    }
    
    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs, boolean ignoreConflicts) {
        return operations.saveOrUpdate(docs, ignoreConflicts);
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    @SafeVarargs
    public final List<CouchDbBulkResponse> saveOrUpdate(Map<String, Object>... docs) {
        return operations.saveOrUpdate(docs);
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<CouchDbBulkResponse> delete(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        return operations.delete(docRev, docRevs);
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<CouchDbBulkResponse> delete(List<CouchDbDocIdAndRev> docRevs) {
        return operations.delete(docRevs);
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Boolean> purge(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        return operations.purge(docRev, docRevs);
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Boolean> purge(List<CouchDbDocIdAndRev> docRevs) {
        return operations.purge(docRevs);
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public CouchDbBulkResponse attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        return operations.attach(docIdAndRev, in, name, contentType);
    }

    /**
     * Attach content to non-existing document.
     */
    public CouchDbBulkResponse attach(String docId, InputStream in, String name, String contentType) {
        return operations.attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }
    
    /**
     * Gets an attachment of the document as String.
     */
    public String getAttachmentAsString(String docId, String name) {
        return operations.getAttachmentAsString(docId, name);
    }
    
    /**
     * Gets an attachment of the document as bytes.
     */
    public byte[] getAttachmentAsBytes(String docId, String name) {
        return operations.getAttachmentAsBytes(docId, name);
    }

    /**
     * Deletes an attachment from the document.
     */
    public boolean deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        return operations.deleteAttachment(docIdAndRev, name);
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return operations.getDesignDocs();
    }
    
    public List<CouchDbDesignDocument> getDesignDocsWithValidators() {
        return operations.getDesignDocsWithValidators();
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public List<String> getDatabases() {
        return operations.getDatabases();
    }

    /**
     * Create a new database.
     */
    public boolean createDb() {
        return operations.createDb();
    }

    /**
     * Delete an existing database.
     */
    public boolean deleteDb() {
        return operations.deleteDb();
    }

    /**
     * Returns database information.
     */
    public CouchDbInfo getInfo() {
        return operations.getInfo();
    }
    
    /**
     * Accessing the root of a CouchDB instance returns meta information about the instance. 
     * The response is a JSON structure containing information about the server, including a 
     * welcome message and the version of the server.     
     */
    public CouchDbInstanceInfo getInstanceInfo() {
        return operations.getInstanceInfo();
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
        return operations.cleanupViews();
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
                logger.warn(e.getMessage(), e);
                logger.warn("Waiting for database...");
                
                try {
                    Thread.sleep(1000);
                } catch (@SuppressWarnings("unused") Exception e1) {
                    System.exit(1);
                }
            } catch (Exception e) {
                logger.error("", e);
                
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
        selfDiscovering = config.isSelfDiscovering();
        enablePurgeListener = config.isEnablePurgeListener();
        httpClientProviderType = config.getHttpClientProviderType();
        
        if (getClass().isAnnotationPresent(com.equiron.acc.annotation.CouchDbConfig.class)) {
            com.equiron.acc.annotation.CouchDbConfig annotationConfig = getClass().getAnnotation(com.equiron.acc.annotation.CouchDbConfig.class);

            _host = annotationConfig.host().isBlank() ? _host : annotationConfig.host();
            _port = annotationConfig.port().isBlank() ? _port : annotationConfig.port();
            _user = annotationConfig.user().isBlank() ? _user : annotationConfig.user();
            _password = annotationConfig.password().isBlank() ? _password : annotationConfig.password();
            _dbName = annotationConfig.dbName().isBlank() ? _dbName : annotationConfig.dbName();
            
            selfDiscovering = annotationConfig.selfDiscovering();
            leaveStaleReplications = annotationConfig.leaveStaleReplications();
            enablePurgeListener = annotationConfig.enablePurgeListener();
            httpClientProviderType = annotationConfig.httpClientProviderType();
            
            String enabledParam = resolve(annotationConfig.enabled(), true);
            
            enabled = !enabledParam.isBlank() && enabledParam.equalsIgnoreCase("true");
        }
        
        if (enabled) {
            this.host = resolve(_host, false);
            this.port = Integer.parseInt(resolve(_port, false));
            this.user = resolve(_user, true);
            this.password = resolve(_password, true);
        }
        
        this.dbName = resolve(_dbName, true);//need for logging
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

            CouchDbView injectedView = null;

            if (viewClass == CouchDbMapView.class) {
                injectedView = new CouchDbMapView<>(this, designName, viewName, jts);
            }

            if (viewClass == CouchDbReduceView.class) {
                injectedView = new CouchDbReduceView<>(this, designName, viewName, jts);
            }

            if (viewClass == CouchDbMapReduceView.class) {
                injectedView = new CouchDbMapReduceView<>(this, designName, viewName, jts);
            }

            if (injectedView != null) {
                if (config.isBuildViewsOnStart()) {
                    updateView(injectedView, true);
                }
                
                setValue(field, injectedView);
                viewList.add(injectedView);
            }
        }
    }

    private void updateView(CouchDbView view, boolean debugEnable) {
        long t = System.currentTimeMillis();
        
        if (debugEnable) {
            logger.info("Updating view in database: " + getDbName() + ". View: " + view.getDesignName() + "/" + view.getViewName() + "...");
        }
        
        boolean complete = false;
        
        while (!complete) {
            try {
                view.update();
                complete = true;
            } catch (Exception e) {
                if (e instanceof IOException) {
                    logger.warn("Not critical exception: " + e.getMessage());
                }
            }
        }
        
        if (debugEnable) {
            logger.info("Complete updating view in database: " + getDbName() + ". View: " + view.getDesignName() + "/" + view.getViewName()  + " in " + (System.currentTimeMillis() - t) + " ms");
        }
    }

    private void injectValidators() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(ValidateDocUpdate.class)) {
                ValidateDocUpdate vdu = field.getAnnotation(ValidateDocUpdate.class);

                setValue(field, new CouchDbValidator(vdu.value()));
            }
        }
    }
        
    private void addSecurity() {
        try {
            CouchDbSecurityObject oldSecurityObject = getSecurityObject();
            
            if (getClass().isAnnotationPresent(Security.class)) {
                SecurityPattern adminsPattern = getClass().getAnnotation(Security.class).admins();
                SecurityPattern membersPattern = getClass().getAnnotation(Security.class).members();
                
                CouchDbSecurityObject securityObject = new CouchDbSecurityObject();
                
                securityObject.setAdmins(new CouchDbSecurityPattern(new HashSet<>(Arrays.asList(adminsPattern.names())), new HashSet<>(Arrays.asList(adminsPattern.roles()))));
                securityObject.setMembers(new CouchDbSecurityPattern(new HashSet<>(Arrays.asList(membersPattern.names())), new HashSet<>(Arrays.asList(membersPattern.roles()))));
                
                if (!oldSecurityObject.equals(securityObject)) {
                    putSecurityObject(securityObject);
                }
            } else {
                CouchDbSecurityObject defaultSecurityObject = new CouchDbSecurityObject();
                
                if (!oldSecurityObject.equals(defaultSecurityObject)) {
                    putSecurityObject(defaultSecurityObject);//clean security object
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void synchronizeReplicationDocs() {
        Map<String, CouchDbReplicationDocument> newReplicationDocs = new HashMap<>();

        for (Replicated replicated : getClass().getAnnotationsByType(Replicated.class)) {
            String _enabled = resolve(replicated.enabled(), true);
            
            if (!_enabled.isBlank() && _enabled.equalsIgnoreCase("true")) {
                String protocol = resolve(replicated.targetProtocol(), false);
                String _host = resolve(replicated.targetHost(), false);
                String _port = resolve(replicated.targetPort(), false);
                String _user = resolve(replicated.targetUser(), true);
                String _password = resolve(replicated.targetPassword(), true);
                String remoteDb = resolve(replicated.targetDbName(), true);
                String selector = resolve(replicated.selector(), true);
                String createTarget = resolve(replicated.createTarget(), true);
                
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
                    CouchDbReplicationDocument toRemote = new CouchDbReplicationDocument(getDbName() + ">>>" + protocol + "://" + _host + ":" + _port + "/" + remoteDb, localServer, remoteServer, selectorMap);
                    
                    if (!createTarget.isBlank() && createTarget.equalsIgnoreCase("true")) {
                        toRemote.setCreateTarget(true);
                    }
                    
                    newReplicationDocs.put(toRemote.getDocId(), toRemote);
                }
                
                if (replicated.direction() == Direction.FROM || replicated.direction() == Direction.BOTH) {
                    CouchDbReplicationDocument fromRemote = new CouchDbReplicationDocument(getDbName() + "<<<" + protocol + "://" + _host + ":" + _port + "/" + remoteDb, remoteServer, localServer, selectorMap);
                    
                    if (!createTarget.isBlank() && createTarget.equalsIgnoreCase("true")) {
                        fromRemote.setCreateTarget(true);
                    }
                    
                    newReplicationDocs.put(fromRemote.getDocId(), fromRemote);
                }
                
                switch (replicated.direction()) {
                    case TO:   logger.info("Prepare one way replication: {} -> {} ",   localServerWithoutCreds,  remoteServerWithoutCreds); break;
                    case FROM: logger.info("Prepare one way replication: {} -> {} ",   remoteServerWithoutCreds, localServerWithoutCreds);  break;
                    case BOTH: logger.info("Prepare two ways replication: {} <-> {} ", localServerWithoutCreds,  remoteServerWithoutCreds); break;
                    default: throw new IllegalStateException();
                }
            } else {
                logger.warn("Replication for " + getDbName() + " disabled");
            }
        }
        
        CouchDbConfig _config = new CouchDbConfig.Builder().setHost(host)
                                                           .setPort(port)
                                                           .setUser(user)
                                                           .setPassword(password)
                                                           .build();
        
        try (ReplicatorDb replicatorDb = new ReplicatorDb(_config)) {        
            List<String> replicationsDocsIds = replicatorDb.getBuiltInView().createQuery()
                                                                            .asIds()
                                                                            .stream()
                                                                            .filter(id -> !id.startsWith("_design/"))//exclude design docs
                                                                            .collect(Collectors.toList());
            
            List<CouchDbReplicationDocument> replicationsDocs = new ArrayList<>();
            
            for (String docId : replicationsDocsIds) {
                try {
                    replicationsDocs.add(replicatorDb.get(docId));
                } catch (Exception e) {
                    logger.warn("Can't parse replication document: " + e.getMessage());
                }
            }
            
            for (var doc : replicationsDocs) {
                if (doc.getDocId().startsWith(getDbName() + "<<<") || doc.getDocId().startsWith(getDbName() + ">>>")) {//находим документы связанные с этой базой
                    if (newReplicationDocs.containsKey(doc.getDocId())) {//значит надо просто обновить
                        CouchDbReplicationDocument updatedReplicationDoc = newReplicationDocs.get(doc.getDocId());
                        
                        if (!updatedReplicationDoc.equals(doc)) {
                            updatedReplicationDoc.setRev(doc.getRev());
                        
                            replicatorDb.saveOrUpdate(updatedReplicationDoc);
                     
                            if (!updatedReplicationDoc.isOk()) {
                                logger.warn("Update replication failed: " + updatedReplicationDoc.getConflictReason());
                            }
                        }
                        
                        newReplicationDocs.remove(doc.getDocId());
                    } else {
                        if (!leaveStaleReplications) {
                            replicatorDb.delete(doc.getDocIdAndRev());
                        }                        
                    }
                }
            }
            
            if (!newReplicationDocs.isEmpty()) {
                replicatorDb.saveOrUpdate(new ArrayList<>(newReplicationDocs.values()));
                
                for (var d : newReplicationDocs.values()) {
                    if (!d.isOk()) {
                        logger.warn("Add replication failed: " + d.getConflictReason());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolve(String param, boolean emptyIfNotResolve) {        
        if (param != null) {
            Pattern r = Pattern.compile("e:\\w+");
                
            Matcher m = r.matcher(param);
                
            if (m.find()) {
                String group = m.group(0);
                    
                String placeholder = group.split(":")[1];
                    
                Optional<String> value = findEnvValue(placeholder);
                    
                if (value.isPresent()) {
                    return param.replace(group, value.get());
                } 
                
                if (emptyIfNotResolve) {
                    return "";
                }
                    
                throw new IllegalStateException("Environment variable or system property not found: " + placeholder);
            } else if (ctx != null) {
                try {
                    return (String) resolveExpression(param);
                } catch (@SuppressWarnings("unused") Exception e) {
                    //empty
                }
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

    private Optional<String> findEnvValue(String propName) {
        Optional<String> value = Stream.of(System.getProperty(propName),
                                           System.getProperty(propName.toLowerCase()), 
                                           System.getProperty(propName.toUpperCase()),
                                           System.getenv(propName),
                                           System.getenv(propName.toLowerCase()), 
                                           System.getenv(propName.toUpperCase())
                                           ).filter(v -> v != null).findFirst();
        return value;
    }

    public void putSecurityObject(CouchDbSecurityObject securityObject) {
        Sneaky.sneak(() -> {
            if (httpClientProvider.put(new UrlBuilder(getDbUrl()).addPathSegment("_security").build(), mapper.writeValueAsString(securityObject)).getStatus() != 200) {
                throw new RuntimeException("Can't apply security");
            }
            
            return true;
        });
    }

    public CouchDbSecurityObject getSecurityObject() {
        return Sneaky.sneak(() -> mapper.readValue(httpClientProvider.get(new UrlBuilder(getDbUrl()).addPathSegment("_security").build()).getBody(), CouchDbSecurityObject.class));
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
            if (field.isAnnotationPresent(JsView.class) || field.isAnnotationPresent(ErlangView.class)) {
                String viewName = NamedStrategy.addUnderscores(field.getName());
                String designName = "_design/" + viewName;
                
                String map = null;
                String reduce = null;
                CouchDbDesignDocument designDocument = null;

                if (field.isAnnotationPresent(JsView.class)) {
                    JsView view = field.getAnnotation(JsView.class);

                    map = "function(doc) {" + view.map() + ";}";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(JsView.COUNT, JsView.STATS, JsView.SUM).contains(view.reduce())) {
                            reduce = view.reduce();
                        } else {
                            reduce = "function(key, values, rereduce) {" + view.reduce() + ";}";
                        }
                    }
                    
                    designDocument = new CouchDbDesignDocument(designName);
                } else {
                    ErlangView view = field.getAnnotation(ErlangView.class);

                    map = "fun({Doc}) -> " + view.map() + " end.";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(ErlangView.COUNT, ErlangView.STATS, ErlangView.SUM).contains(view.reduce())) {
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
            
            if (field.isAnnotationPresent(ValidateDocUpdate.class)) {
                field.setAccessible(true);

                ValidateDocUpdate vdu = field.getAnnotation(ValidateDocUpdate.class);

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
        if (!purgeListener.isStopped()) {
            purgeListener.stopListening();
        }
    }
}