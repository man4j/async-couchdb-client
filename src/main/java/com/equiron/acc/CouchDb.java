package com.equiron.acc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.equiron.acc.annotation.JsView;
import com.equiron.acc.annotation.Replicated;
import com.equiron.acc.annotation.Replicated.Direction;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.annotation.ValidateDocUpdate;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.exception.CouchDbResponseException;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbInfo;
import com.equiron.acc.json.CouchDbReplicationDocument;
import com.equiron.acc.json.security.CouchDbSecurityObject;
import com.equiron.acc.json.security.CouchDbSecurityPattern;
import com.equiron.acc.util.ExceptionHandler;
import com.equiron.acc.util.NamedStrategy;
import com.equiron.acc.util.ReflectionUtils;
import com.equiron.acc.util.UrlBuilder;
import com.equiron.acc.view.CouchDbBuiltInView;
import com.equiron.acc.view.CouchDbMapReduceView;
import com.equiron.acc.view.CouchDbMapView;
import com.equiron.acc.view.CouchDbReduceView;
import com.equiron.acc.view.CouchDbView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CouchDb implements AutoCloseable {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private volatile CouchDbConfig config;

    final ObjectMapper mapper = new ObjectMapper();

    volatile Request prototype;
    
    private volatile String ip;
    
    private volatile int port;
    
    private volatile String user;
    
    private volatile String password;
    
    private volatile String dbName;
    
    private volatile boolean enabled = true;
    
    private volatile CouchDbBuiltInView builtInView;

    private volatile CouchDbAsyncOperations asyncOps;
    
    private volatile boolean selfDiscovering = true;
    
    private volatile boolean initialized;
    
    private volatile List<CouchDbView> viewList = new CopyOnWriteArrayList<>();
    
    private volatile Thread updateViewThread;
    
    @PostConstruct
    public void init() {
        if (!initialized) {//может быть инициализирована в конструкторе
            mapper.registerModule(new JavaTimeModule());
            
            applyConfig();
            
            if (enabled) {
                RequestBuilder builder = new RequestBuilder().setHeader("Content-Type", "application/json; charset=utf-8")
                                                             .setCharset(StandardCharsets.UTF_8);
        
                if (this.user != null && this.password != null) {
                    Realm realm = new Realm.Builder(this.user, this.password)
                                           .setUsePreemptiveAuth(true)
                                           .setScheme(AuthScheme.BASIC)
                                           .build();
        
                    builder.setRealm(realm);
                }
        
                prototype = builder.build();
                
                asyncOps = new CouchDbAsyncOperations(this);
                
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

                    updateViewThread = new Thread("CouchDB view updater for: " + getDbName()) {
                        @Override
                        public void run() {
                            while (!Thread.interrupted()) {
                                try {
                                    updateAllViews();
                                } catch (Exception e) {
                                    logger.error("Can't update views: " + e.getMessage());
                                }
                                try {
                                    Thread.sleep(60_000);
                                } catch (@SuppressWarnings("unused") InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    };
                    
                    updateViewThread.start();
                }
                
                initialized = true;
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
    
    public Request getPrototype() {
        return prototype;
    }
    
    public AsyncHttpClient getHttpClient() {
        return config.getHttpClient();
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
        return String.format("http://%s:%s", ip, port);
    }

    public String getDbUrl() {
        return new UrlBuilder(getServerUrl()).addPathSegment(getDbName()).toString();
    }
    
    public CouchDbBuiltInView getBuiltInView() {
        return builtInView;
    }

    public CouchDbAsyncOperations async() {
        return asyncOps;
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
        return ExceptionHandler.handleFutureResult(asyncOps.get(docId));
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId) {
        return ExceptionHandler.handleFutureResult(asyncOps.getRaw(docId));
    }
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(String docId, boolean attachments) {
        return ExceptionHandler.handleFutureResult(asyncOps.get(docId, attachments));
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        return ExceptionHandler.handleFutureResult(asyncOps.getRaw(docId, attachments));
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(doc, docs));
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public List<CouchDbBulkResponse> saveOrUpdate(@SuppressWarnings("unchecked") Map<String, Object>... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<CouchDbBulkResponse> delete(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(docRev, docRevs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public List<CouchDbBulkResponse> delete(List<CouchDbDocIdAndRev> docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(docRevs));
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Boolean> purge(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.purge(docRev, docRevs));
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Boolean> purge(List<CouchDbDocIdAndRev> docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.purge(docRevs));
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public CouchDbBulkResponse attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        return ExceptionHandler.handleFutureResult(asyncOps.attach(docIdAndRev, in, name, contentType));
    }

    /**
     * Attach content to non-existing document.
     */
    public CouchDbBulkResponse attach(String docId, InputStream in, String name, String contentType) {
        return attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }

    /**
     * Gets an attachment of the document as Response.
     */
    public Response getAttachment(String docId, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.getAttachment(docId, name));
    }
    
    /**
     * Gets an attachment of the document as String.
     */
    public String getAttachmentAsString(String docId, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.getAttachmentAsString(docId, name));
    }
    
    /**
     * Gets an attachment of the document as bytes.
     */
    public byte[] getAttachmentAsBytes(String docId, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.getAttachmentAsBytes(docId, name));
    }


    /**
     * Deletes an attachment from the document.
     */
    public boolean deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.deleteAttachment(docIdAndRev, name));
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return ExceptionHandler.handleFutureResult(asyncOps.getDesignDocs());
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public List<String> getDatabases() {
        return ExceptionHandler.handleFutureResult(asyncOps.getDatabases());
    }

    /**
     * Create a new database.
     */
    public boolean createDb() {
        return ExceptionHandler.handleFutureResult(asyncOps.createDb());
    }

    /**
     * Delete an existing database.
     */
    public boolean deleteDb() {
        return ExceptionHandler.handleFutureResult(asyncOps.deleteDb());
    }

    /**
     * Returns database information.
     */
    public CouchDbInfo getInfo() {
        return ExceptionHandler.handleFutureResult(asyncOps.getInfo());
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
        return ExceptionHandler.handleFutureResult(asyncOps.cleanupViews());
    }

    //------------------ Discovering methods -------------------------

    private void testConnection() {
        while (true) {
            try {
                if (config.getHttpClient().prepareRequest(prototype)
                                          .setMethod("GET")
                                          .setUrl(getServerUrl())
                                          .execute().get().getStatusCode() != 200) {
                    throw new ConnectException("Could not connect to " + getServerUrl());
                }
                
                break;
            } catch (ExecutionException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    logger.warn(e.getMessage(), e);
                    logger.warn("Waiting for database...");
                        
                    try {
                        Thread.sleep(1000);
                    } catch (@SuppressWarnings("unused") Exception e1) {
                        System.exit(1);
                    }                        
                } else {
                    logger.error("", e);
                        
                    System.exit(1);
                }
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
        String ip = config.getIp();
        String port = config.getPort() + "";
        String user = config.getUser();
        String password = config.getPassword();
        String dbName = config.getDbName() == null ? NamedStrategy.addUnderscores(getClass().getSimpleName()) : config.getDbName();
        selfDiscovering = config.isSelfDiscovering();
        
        if (getClass().isAnnotationPresent(com.equiron.acc.annotation.CouchDbConfig.class)) {
            com.equiron.acc.annotation.CouchDbConfig annotationConfig = getClass().getAnnotation(com.equiron.acc.annotation.CouchDbConfig.class);

            ip = annotationConfig.ip().isBlank() ? ip : annotationConfig.ip();
            port = annotationConfig.port().isBlank() ? port : annotationConfig.port();
            user = annotationConfig.user().isBlank() ? user : annotationConfig.user();
            password = annotationConfig.password().isBlank() ? password : annotationConfig.password();
            dbName = annotationConfig.dbName().isBlank() ? dbName : annotationConfig.dbName();
            
            selfDiscovering = annotationConfig.selfDiscovering();
            
            String enabledParam = resolve(annotationConfig.enabled(), true);
            
            enabled = !enabledParam.isBlank() && enabledParam.equalsIgnoreCase("true");
        }
        
        if (enabled) {
            this.ip = resolve(ip, false);
            this.port = Integer.parseInt(resolve(port, false));
            this.user = resolve(user, true);
            this.password = resolve(password, true);
        }
        
        this.dbName = resolve(dbName, true);//need for logging
    }

    private void createDbIfNotExist() {
        if (!getDatabases().contains(getDbName())) {
            createDb();
        }
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
            } catch (CouchDbResponseException e) {
                logger.warn("Not critical exception: " + e.getMessage());
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
            @SuppressWarnings("resource")
            AsyncHttpClient client = config.getHttpClient();
            
            CouchDbSecurityObject oldSecurityObject = getSecurityObject(client);
            
            if (getClass().isAnnotationPresent(Security.class)) {
                SecurityPattern adminsPattern = getClass().getAnnotation(Security.class).admins();
                SecurityPattern membersPattern = getClass().getAnnotation(Security.class).members();
                
                CouchDbSecurityObject securityObject = new CouchDbSecurityObject();
                
                securityObject.setAdmins(new CouchDbSecurityPattern(new HashSet<>(Arrays.asList(adminsPattern.names())), new HashSet<>(Arrays.asList(adminsPattern.roles()))));
                securityObject.setMembers(new CouchDbSecurityPattern(new HashSet<>(Arrays.asList(membersPattern.names())), new HashSet<>(Arrays.asList(membersPattern.roles()))));
                
                if (!oldSecurityObject.equals(securityObject)) {
                    putSecurityObject(client, securityObject);
                }
            } else {
                if (!oldSecurityObject.getAdmins().getNames().isEmpty()
                 || !oldSecurityObject.getAdmins().getRoles().isEmpty()
                 || !oldSecurityObject.getMembers().getNames().isEmpty()
                 || !oldSecurityObject.getMembers().getRoles().isEmpty()) {
                    putSecurityObject(client, new CouchDbSecurityObject());//clean security object
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void synchronizeReplicationDocs() {
        Map<String, CouchDbReplicationDocument> newReplicationDocs = new HashMap<>();
        
        for (Replicated replicated : getClass().getAnnotationsByType(Replicated.class)) {            
            String enabled = replicated.enabled();
            
            enabled = resolve(enabled, true);
            
            if (!enabled.isBlank() && enabled.equalsIgnoreCase("true")) {
                String ip = resolve(replicated.targetIp(), false);
                String port = resolve(replicated.targetPort(), false);
                String user = resolve(replicated.targetUser(), true);
                String password = resolve(replicated.targetPassword(), true);
                String remoteDb = resolve(replicated.targetDbName(), true);
                String selector = resolve(replicated.selector(), true);
                String createTarget = resolve(replicated.createTarget(), true);
                
                if (remoteDb.isBlank()) {
                    remoteDb = getDbName();
                }
                
                String remoteServer;
                
                if (user.isBlank() && password.isBlank()) {
                    remoteServer = String.format("http://%s:%s/%s", ip, port, remoteDb);
                } else {
                    remoteServer = String.format("http://%s:%s@%s:%s/%s", user, password, ip, port, remoteDb);
                }
                
                String localServer;
                
                if (this.user == null && this.password == null) {
                    localServer = String.format("http://%s:%s/%s", this.ip, this.port, getDbName());
                } else {
                    localServer = String.format("http://%s:%s@%s:%s/%s", this.user, this.password, this.ip, this.port, getDbName());
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
                    CouchDbReplicationDocument toRemote = new CouchDbReplicationDocument(getDbName() + "$" + remoteDb + "_to", localServer, remoteServer, selectorMap);
                    
                    if (!createTarget.isBlank() && createTarget.equalsIgnoreCase("true")) {
                        toRemote.setCreateTarget(true);
                    }
                    
                    newReplicationDocs.put(toRemote.getDocId(), toRemote);
                }
                
                if (replicated.direction() == Direction.FROM || replicated.direction() == Direction.BOTH) {
                    CouchDbReplicationDocument fromRemote = new CouchDbReplicationDocument(getDbName() + "$" + remoteDb + "_from", remoteServer, localServer, selectorMap);
                    
                    if (!createTarget.isBlank() && createTarget.equalsIgnoreCase("true")) {
                        fromRemote.setCreateTarget(true);
                    }
                    
                    newReplicationDocs.put(fromRemote.getDocId(), fromRemote);
                }
                
                switch (replicated.direction()) {
                    case TO:   logger.info("Prepare one way replication: {} -> {} ",   localServer,  remoteServer); break;
                    case FROM: logger.info("Prepare one way replication: {} -> {} ",   remoteServer, localServer);  break;
                    case BOTH: logger.info("Prepare two ways replication: {} <-> {} ", localServer,  remoteServer); break;
                    default: throw new IllegalStateException();
                }
            } else {
                logger.warn("Replication for " + getDbName() + " disabled");
            }
        }
        
        CouchDbConfig config = new CouchDbConfig.Builder().setHttpClient(getHttpClient())
                                                          .setIp(ip)
                                                          .setPort(port)
                                                          .setUser(user)
                                                          .setPassword(password)
                                                          .build();
        
        try (ReplicatorDb replicatorDb = new ReplicatorDb(config)) {        
            List<String> replicationsDocsIds = replicatorDb.getBuiltInView().createQuery()
                                                                            .asIds()
                                                                            .stream()
                                                                            .filter(id -> !id.startsWith("_design/"))//exclude design docs
                                                                            .collect(Collectors.toList());
            
            List<CouchDbReplicationDocument> replicationsDocs = replicatorDb.getBuiltInView().<CouchDbReplicationDocument>createDocQuery().byKeys(replicationsDocsIds).asDocs();
            
            for (var doc : replicationsDocs) {
                if (doc.getDocId().startsWith(getDbName() + "$")) {//находим документы связанные с этой базой
                    if (newReplicationDocs.containsKey(doc.getDocId())) {//значит надо просто обновить
                        CouchDbReplicationDocument updatedReplicationDoc = newReplicationDocs.get(doc.getDocId());
                        
                        if (!updatedReplicationDoc.equals(doc)) {
                            updatedReplicationDoc.setRev(doc.getRev());
                        
                            replicatorDb.saveOrUpdate(updatedReplicationDoc);
                     
                            if (updatedReplicationDoc.isOk()) {
                                logger.info("Updated replication " + updatedReplicationDoc.getSource() + " -> " + updatedReplicationDoc.getTarget() + ": [OK]");
                            } else {
                                logger.info("Updated replication " + updatedReplicationDoc.getSource() + " -> " + updatedReplicationDoc.getTarget() + ": [" + updatedReplicationDoc.getConflictReason() + "]");
                            }
                        }
                        
                        newReplicationDocs.remove(doc.getDocId());
                    } else {
                        replicatorDb.delete(doc.getDocIdAndRev());
                    }
                }
            }
            
            if (!newReplicationDocs.isEmpty()) {
                replicatorDb.saveOrUpdate(new ArrayList<>(newReplicationDocs.values()));
                
                for (var d : newReplicationDocs.values()) {
                    if (d.isOk()) {
                        logger.info("Add replication " + d.getSource() + " -> " + d.getTarget() + ": [OK]");
                    } else {
                        logger.info("Add replication " + d.getSource() + " -> " + d.getTarget() + ": [" + d.getConflictReason() + "]");
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
            }
        }
        
        return param;
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

    private void putSecurityObject(AsyncHttpClient client, CouchDbSecurityObject securityObject) throws InterruptedException, ExecutionException, JsonProcessingException {
        Response r = client.prepareRequest(prototype).setMethod("PUT")
                                                     .setUrl(new UrlBuilder(getDbUrl())
                                                     .addPathSegment("_security").build())
                                                     .setBody(mapper.writeValueAsString(securityObject))
                                                     .execute()
                                                     .get();
        
        if (r.getStatusCode() != 200) throw new RuntimeException("Can't apply security");
    }

    private CouchDbSecurityObject getSecurityObject(AsyncHttpClient client) throws IOException, InterruptedException, ExecutionException {
        return mapper.readValue(client.prepareRequest(prototype).setMethod("GET")
                                                                .setUrl(new UrlBuilder(getDbUrl())
                                                                .addPathSegment("_security")
                                                                .build())
                                                                .execute()
                                                                .get()
                                                                .getResponseBody(StandardCharsets.UTF_8), CouchDbSecurityObject.class);
    }
    
    private void synchronizeDesignDocs() {
        Set<CouchDbDesignDocument> oldDesignDocs = new HashSet<>(getDesignDocs());

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
            if (field.isAnnotationPresent(JsView.class)) {
                JsView view = field.getAnnotation(JsView.class);

                String viewName = NamedStrategy.addUnderscores(field.getName());
                String designName = "_design/" + viewName;
                
                String map = "function(doc) {" + view.map() + ";}";

                String reduce = null;

                if (!view.reduce().isEmpty()) {
                    if (Arrays.asList(JsView.COUNT, JsView.STATS, JsView.SUM).contains(view.reduce())) {
                        reduce = view.reduce();
                    } else {
                        reduce = "function(key, values, rereduce) {" + view.reduce() + ";}";
                    }
                }

                CouchDbDesignDocument designDocument = new CouchDbDesignDocument(designName);
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
        if (updateViewThread != null) {
            updateViewThread.interrupt();
            updateViewThread.join();
        }
    }
}