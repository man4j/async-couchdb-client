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

import org.apache.commons.lang3.ArrayUtils;
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
import com.equiron.acc.cache.YnsCachedDocumentOperations;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.exception.YnsBulkDocumentException;
import com.equiron.acc.json.YnsBulkGetResponse;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDbInfo;
import com.equiron.acc.json.YnsDbInfo.YnsClusterInfo;
import com.equiron.acc.json.YnsDesignDocument;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsInstanceInfo;
import com.equiron.acc.json.YnsReplicationDocument;
import com.equiron.acc.json.security.YnsSecurityObject;
import com.equiron.acc.profiler.OperationType;
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
    @Autowired
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
    
    private volatile boolean removeNotDeclaredReplications = false;
    
    private volatile boolean enableDocumentCache = false;
    
    private volatile int cacheMaxDocsCount;
    
    private volatile int cacheMaxTimeoutSec;
    
    private volatile List<YnsView> viewList = new CopyOnWriteArrayList<>();
    
    private volatile YnsClusterInfo clusterInfo;
    
    private volatile HttpClientProvider httpClientProvider;
    
    public YnsDb() {
        //empty
    }
    
    public YnsDb(com.equiron.acc.YnsDbConfig config) {
        this.config = config;

        mapper.registerModule(new JavaTimeModule());
        
        applyConfig();
        
        httpClientProvider = httpClientProviderType == HttpClientProviderType.JDK ? new JdkHttpClientProvider() : new OkHttpClientProvider();
        
        if (user != null && password != null) {
            httpClientProvider.setCredentials(user, password);
        }

        if (enableDocumentCache) {
            operations = new YnsCachedDocumentOperations(new YnsDocumentOperations(this), cacheMaxDocsCount, cacheMaxTimeoutSec);
        } else {
            operations = new YnsDocumentOperations(this);
        }
                                                              
        adminOperations = new YnsAdminOperations(this);

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
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> T get(String docId, Class<T> clazz) {
        List<T> docs = get(List.of(docId), clazz);

        return docs.isEmpty() ? null : docs.get(0);
    }
            
    /**
     * Returns the latest revision of the documents.
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> List<T> get(List<String> docIds, Class<T> clazz) {
        TypeFactory tf = TypeFactory.defaultInstance();
        
        List<T> result = operations.get(docIds.stream().map(id -> new YnsDocIdAndRev(id, null)).toList(), tf.constructParametricType(YnsBulkGetResponse.class, clazz), false);
        
        return result;
    }
    
    /**
     * Returns the latest revision of the document.
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> T get(String docId, TypeReference<T> type) {
        List<T> docs = get(List.of(docId), type);

        return docs.isEmpty() ? null : docs.get(0);
    }
            
    /**
     * Returns the latest revision of the documents.
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> List<T> get(List<String> docIds, TypeReference<T> type) {
        TypeFactory tf = TypeFactory.defaultInstance();
        
        List<T> result = operations.get(docIds.stream().map(id -> new YnsDocIdAndRev(id, null)).toList(), tf.constructParametricType(YnsBulkGetResponse.class, tf.constructType(type)), false);
        
        return result;
    }
    
    /**
     * Returns specified revision of the document.
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> T getWithRev(YnsDocIdAndRev docId, Class<T> clazz) {
        List<T> docs = getWithRev(List.of(docId), clazz);

        return docs.isEmpty() ? null : docs.get(0);
    }
        
    /**
     * Returns specified revision of the documents.
     * @throws YnsBulkGetException
     */
    public <T extends YnsDocument> List<T> getWithRev(List<YnsDocIdAndRev> docIds, Class<T> clazz) {
        TypeFactory tf = TypeFactory.defaultInstance();

        List<T> result = operations.get(docIds, tf.constructParametricType(YnsBulkGetResponse.class, clazz), false);
        
        return result;
    }
    
    //------------------ Fetch RAW API -------------------------
    
    /**
     * Returns the latest revision of the document.
     * @throws YnsBulkGetException
     */
    public Map<String, Object> getRaw(String docId) {
        List<Map<String, Object>> docs = getRaw(List.of(docId));

        return docs.isEmpty() ? null : docs.get(0);
    }

    /**
     * Returns the latest revision of the documents.
     * @throws YnsBulkGetException
     */
    public List<Map<String, Object>> getRaw(List<String> docIds) {
        TypeFactory tf = TypeFactory.defaultInstance();

        JavaType javaType = tf.constructParametricType(YnsBulkGetResponse.class, tf.constructMapType(Map.class, String.class, Object.class));
        
        return operations.get(docIds.stream().map(id -> new YnsDocIdAndRev(id, null)).toList(), javaType, true);
    }
    
    /**
     * Returns specified revision of the document.
     * @throws YnsBulkGetException
     */
    public Map<String, Object> getRawWithRev(YnsDocIdAndRev docId) {
        List<Map<String, Object>> docs = getRawWithRev(List.of(docId));

        return docs.isEmpty() ? null : docs.get(0);
    }

    /**
     * Returns specified revision of the documents.
     * @throws YnsBulkGetException
     */
    public List<Map<String, Object>> getRawWithRev(List<YnsDocIdAndRev> docIds) {
        TypeFactory tf = TypeFactory.defaultInstance();

        JavaType javaType = tf.constructParametricType(YnsBulkGetResponse.class, tf.constructMapType(Map.class, String.class, Object.class));

        return operations.get(docIds, javaType, true);
    }

    //------------------ Bulk API -------------------------
    
    /**
    * Insert or update multiple documents in to the database in a single request.
    * @throws YnsBulkException 
    */
    final public <T extends YnsDocument> String saveOrUpdate(T doc) {
        saveOrUpdate(Arrays.asList(doc));
        
        return doc.getDocId();
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     * @throws YnsBulkException 
     */
    public <T extends YnsDocument> List<String> saveOrUpdate(List<T> docs) {
        operations.saveOrUpdate(docs, OperationType.INSERT_UPDATE, false);
        
        return docs.stream().map(YnsDocument::getDocId).toList();
    }
    
    //------------------ Save or update RAW API -------------------------

    /**
     * Insert or update multiple documents in to the database in a single request.
     * @throws YnsBulkException 
     */
    final public String saveOrUpdateRaw(Map<String, Object> doc) {
        saveOrUpdateRaw(Arrays.asList(doc));
        
        return (String) doc.get("_id");
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     * @throws YnsBulkException 
     */
    public List<String> saveOrUpdateRaw(List<Map<String, Object>> docs) {
        operations.saveOrUpdate(docs, OperationType.INSERT_UPDATE, true);
        
        return docs.stream().map(d -> (String) d.get("_id")).toList();
    }
    
    //------------------ Delete API -------------------------
    
    /**
     * Delete multiple documents from the database in a single request.
     * @throws YnsBulkException
     */
    public void delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        delete(Arrays.asList(allDocs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     * @throws YnsBulkException 
     */
    public void delete(List<YnsDocIdAndRev> docRevs) {
        List<YnsDocument> docsWithoutBody = docRevs.stream().map(dr -> {
            YnsDocument dummyDoc = new YnsDocument();
            
            dummyDoc.setDocId(dr.getDocId());
            dummyDoc.setRev(dr.getRev());
            dummyDoc.setDeleted(true);
            
            return dummyDoc;
        }).toList();
        
        operations.saveOrUpdate(docsWithoutBody, OperationType.DELETE, false);
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
        return getAttachmentAsStream(docId, name, null);
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

    public List<YnsDesignDocument> getDesignDocs() {
        return adminOperations.getDesignDocs();
    }
    
    public List<YnsDesignDocument> getDesignDocsWithValidators() {
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
        String _cacheMaxDocsCount = config.getCacheMaxDocsCount() + "";
        String _cacheMaxTimeoutSec = config.getCacheMaxTimeoutSec() + "";
        
        buildViewOnStart = config.isBuildViewsOnStart();
        selfDiscovering = config.isSelfDiscovering();
        removeNotDeclaredReplications = config.isRemoveNotDeclaredReplications();
        enableDocumentCache = config.isEnableDocumentCache();
        httpClientProviderType = config.getHttpClientProviderType();
        
        if (getClass().isAnnotationPresent(com.equiron.acc.annotation.YnsDbConfig.class)) {
            YnsDbConfig annotationConfig = getClass().getAnnotation(YnsDbConfig.class);

            if (!annotationConfig.host().isBlank()) _host = annotationConfig.host();
            if (!annotationConfig.port().isBlank()) _port = annotationConfig.port();
            if (!annotationConfig.user().isBlank()) _user = annotationConfig.user();
            if (!annotationConfig.password().isBlank()) _password = annotationConfig.password();
            if (!annotationConfig.dbName().isBlank()) _dbName = annotationConfig.dbName();
            if (!annotationConfig.clientMaxParallelism().isBlank()) _clientMaxParallelism = annotationConfig.clientMaxParallelism();
            if (!annotationConfig.cacheMaxDocsCount().isBlank()) _cacheMaxDocsCount = annotationConfig.cacheMaxDocsCount();
            if (!annotationConfig.cacheMaxTimeoutSec().isBlank()) _cacheMaxTimeoutSec = annotationConfig.cacheMaxTimeoutSec();
            if (annotationConfig.buildViewOnStart() != AnnotationConfigOption.BY_CONFIG) buildViewOnStart = annotationConfig.buildViewOnStart() == AnnotationConfigOption.ENABLED ? true : false;
            if (annotationConfig.selfDiscovering() != AnnotationConfigOption.BY_CONFIG) selfDiscovering = annotationConfig.selfDiscovering() == AnnotationConfigOption.ENABLED ? true : false;
            if (annotationConfig.removeNotDeclaredReplications() != AnnotationConfigOption.BY_CONFIG) removeNotDeclaredReplications = annotationConfig.removeNotDeclaredReplications() == AnnotationConfigOption.ENABLED ? true : false;
            if (annotationConfig.enableDocumentCache() != AnnotationConfigOption.BY_CONFIG) enableDocumentCache = annotationConfig.enableDocumentCache() == AnnotationConfigOption.ENABLED ? true : false;
        }
        
        this.host = resolve(_host);
        this.port = Integer.parseInt(resolve(_port));
        this.user = resolve(_user);
        this.password = resolve(_password);
        this.dbName = resolve(_dbName);
        this.clientMaxParallelism = Integer.parseInt(resolve(_clientMaxParallelism));
        this.cacheMaxDocsCount = Integer.parseInt(resolve(_cacheMaxDocsCount));
        this.cacheMaxTimeoutSec = Integer.parseInt(resolve(_cacheMaxTimeoutSec));
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
                    replicationsDocs.add(replicatorDb.get(docId, YnsReplicationDocument.class));
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
                        
                            try {
                                replicatorDb.saveOrUpdate(updatedReplicationDoc);
                            } catch (YnsBulkDocumentException e) {
                                YnsBulkResponse resp = e.getResponses().get(0);
                                
                                if (!resp.isOk()) {
                                    log.warn("Update replication failed: " + resp.getConflictReason() + ", " + resp.getError());
                                }
                            }
                        }
                        
                        newReplicationDocs.remove(doc.getDocId());
                    } else if (removeNotDeclaredReplications) {
                        replicatorDb.delete(doc.getDocIdAndRev());
                    }
                }
            }
            
            if (!newReplicationDocs.isEmpty()) {
                try {
                    replicatorDb.saveOrUpdate(new ArrayList<>(newReplicationDocs.values()));
                } catch (YnsBulkDocumentException e) {
                    for (var resp : e.getResponses()) {
                        if (!resp.isOk()) {
                            log.warn("Add replication failed: " + resp.getConflictReason() + ", " + resp.getError());
                        }
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
        Set<YnsDesignDocument> oldDesignDocs = new HashSet<>(getDesignDocsWithValidators());

        Set<YnsDesignDocument> newDesignDocs = generateNewDesignDocs();

        for (YnsDesignDocument oldDoc : oldDesignDocs) {
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

    private Set<YnsDesignDocument> generateNewDesignDocs() {
        Set<YnsDesignDocument> designSet = new HashSet<>();

        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(YnsJsView.class) || field.isAnnotationPresent(YnsErlangView.class)) {
                String viewName = NamedStrategy.addUnderscores(field.getName());
                String designName = "_design/" + viewName;
                
                String map = null;
                String reduce = null;
                YnsDesignDocument designDocument = null;

                if (field.isAnnotationPresent(YnsJsView.class)) {
                    YnsJsView view = field.getAnnotation(YnsJsView.class);

                    String funBody = !view.map().isBlank() ? view.map() : view.value();
                    
                    map = "function(doc) {" + funBody + ";}";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(YnsJsView.COUNT, YnsJsView.STATS, YnsJsView.SUM).contains(view.reduce())) {
                            reduce = view.reduce();
                        } else {
                            reduce = "function(key, values, rereduce) {" + view.reduce() + ";}";
                        }
                    }
                    
                    designDocument = new YnsDesignDocument(designName);
                } else {
                    YnsErlangView view = field.getAnnotation(YnsErlangView.class);
                    
                    String funBody = !view.map().isBlank() ? view.map() : view.value();

                    map = "fun({Doc}) -> " + funBody + " end.";
    
                    if (!view.reduce().isEmpty()) {
                        if (Arrays.asList(YnsErlangView.COUNT, YnsErlangView.STATS, YnsErlangView.SUM).contains(view.reduce())) {
                            reduce = view.reduce();
                        } else {
                            reduce = "fun(Keys, Values, ReReduce) -> " + view.reduce() + " end.";
                        }
                    }
                    
                    designDocument = new YnsDesignDocument(designName, "erlang");
                }
                
                designDocument.addView(viewName, map, reduce);
                designSet.add(designDocument);
            }
            
            if (field.isAnnotationPresent(YnsValidateDocUpdate.class)) {
                field.setAccessible(true);

                YnsValidateDocUpdate vdu = field.getAnnotation(YnsValidateDocUpdate.class);

                String designName = "_design/" + NamedStrategy.addUnderscores(field.getName());
                
                YnsDesignDocument designDocument = new YnsDesignDocument(designName);
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