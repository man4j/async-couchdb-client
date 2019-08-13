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
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

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
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.annotation.ValidateDocUpdate;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.exception.CouchDbResponseException;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocRev;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbInfo;
import com.equiron.acc.json.CouchDbReplicationDocument;
import com.equiron.acc.json.security.CouchDbSecurityObject;
import com.equiron.acc.json.security.CouchDbSecurityPattern;
import com.equiron.acc.query.CouchDbMapQueryWithDocs;
import com.equiron.acc.util.ExceptionHandler;
import com.equiron.acc.util.NamedStrategy;
import com.equiron.acc.util.ReflectionUtils;
import com.equiron.acc.util.UrlBuilder;
import com.equiron.acc.view.CouchDbBuiltInView;
import com.equiron.acc.view.CouchDbMapReduceView;
import com.equiron.acc.view.CouchDbMapView;
import com.equiron.acc.view.CouchDbReduceView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CouchDb {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private CouchDbConfig config;

    ObjectMapper mapper = new ObjectMapper();

    Request prototype;
    
    private String ip;
    
    private int port;
    
    private String user;
    
    private String password;
    
    private String dbName;
    
    private CouchDbBuiltInView builtInView;

    private CouchDbAsyncOperations asyncOps;
    
    private boolean selfDiscovering = true;
    
    private boolean initialized;
    
    @PostConstruct
    public void init() {
        if (!initialized) {//может быть инициализирована в конструкторе
            mapper.registerModule(new JavaTimeModule());
            
            applyConfig();

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
            
            testConnection();
    
            createDbIfNotExist();
            
            if (selfDiscovering) {            
                synchronizeDesignDocs();

                injectViews();
    
                injectValidators();

                addSecurity();
    
                cleanupViews();
                
                synchronizeReplicationDocs();
            }
            
            initialized = true;
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

    //------------------ Fetch API -------------------------
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(String docId) {
        return get(docId, false);
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId) {
        return getRaw(docId, false);
    }
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(String docId, boolean attachments) {
        CouchDbMapQueryWithDocs<String, CouchDbDocRev, T> query = builtInView.<T>createDocQuery();
        
        if (attachments) {
            query.includeAttachments();
        }
        
        return query.byKey(docId).asDoc();
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        CouchDbMapQueryWithDocs<String,CouchDbDocRev,Map<String,Object>> query = builtInView.createRawDocQuery();
        
        if (attachments) {
            query.includeAttachments();
        }
        
        return query.byKey(docId).asDoc();
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    @SafeVarargs
    public final <T extends CouchDbDocument> List<T> saveOrUpdate(T doc, T... docs) {
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
    @SafeVarargs
    public final List<CouchDbBulkResponse> saveOrUpdate(Map<String, Object>... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    @SafeVarargs
    public final List<CouchDbBulkResponse> delete(final CouchDbDocIdAndRev docRev, final CouchDbDocIdAndRev... docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(docRev, docRevs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public final List<CouchDbBulkResponse> delete(final List<CouchDbDocIdAndRev> docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(docRevs));
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    @SafeVarargs
    public final Map<String, Boolean> purge(final CouchDbDocIdAndRev docRev, final CouchDbDocIdAndRev... docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.purge(docRev, docRevs));
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Boolean> purge(final List<CouchDbDocIdAndRev> docRevs) {
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
        try {
            if (config.getHttpClient().prepareRequest(prototype)
                                      .setMethod("GET")
                                      .setUrl(getServerUrl())
                                      .execute().get().getStatusCode() != 200) {
                throw new ConnectException("Could not connect to " + getServerUrl());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        }
            
        this.ip = resolve(ip, false);
        this.port = Integer.parseInt(resolve(port, false));
        this.user = resolve(user, true);
        this.password = resolve(password, true);
        this.dbName = resolve(dbName, true);
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

            Object injectedView = null;

            if (viewClass == CouchDbMapView.class) {
                CouchDbMapView<String, Object> view = new CouchDbMapView<>(this, designName, viewName, jts);

                if (config.isBuildViewsOnStart()) {
                    long t = System.currentTimeMillis();
                    logger.info("Building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName + "...");
                    
                    boolean complete = false;
                    
                    while (!complete) {
                        try {
                            view.createQuery().byKey("123").asKey();
                            complete = true;
                        } catch (CouchDbResponseException e) {
                            logger.warn("Not critical exception: " + e.getMessage());
                        }
                    }
                    
                    logger.info("Complete building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " ms");
                }

                injectedView = view;
            }

            if (viewClass == CouchDbReduceView.class) {
                CouchDbReduceView<String, Object> view = new CouchDbReduceView<>(this, designName, viewName, jts);

                if (config.isBuildViewsOnStart()) {
                    long t = System.currentTimeMillis();
                    logger.info("Building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName + "...");
                    
                    boolean complete = false;
                    
                    while (!complete) {
                        try {
                            view.createQuery().byKey("123").asKey();
                            complete = true;
                        } catch (CouchDbResponseException e) {
                            logger.warn("Not critical exception: " + e.getMessage());
                        }
                    }
                    
                    logger.info("Complete building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " ms");
                }

                injectedView = view;
            }

            if (viewClass == CouchDbMapReduceView.class) {
                CouchDbMapReduceView<String, Object, Object, Object> view = new CouchDbMapReduceView<>(this, designName, viewName, jts);

                if (config.isBuildViewsOnStart()) {
                    long t = System.currentTimeMillis();
                    logger.info("Building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName + "...");
                    
                    boolean complete = false;
                    
                    while (!complete) {
                        try {
                            view.createMapQuery().byKey("123").asKey();
                            complete = true;
                        } catch (CouchDbResponseException e) {
                            logger.warn("Not critical exception: " + e.getMessage());
                        }
                    }
                    
                    logger.info("Complete building view in database: " + getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " ms");                        
                }

                injectedView = view;
            }

            if (injectedView != null) {
                setValue(field, injectedView);
            }
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
    
    private void synchronizeReplicationDocs() {
        Map<String, CouchDbReplicationDocument> newReplicationDocs = new HashMap<>();
        
        if (getClass().isAnnotationPresent(Replicated.class)) {
            String enabled = getClass().getAnnotation(Replicated.class).enabled();
            
            enabled = resolve(enabled, true);
            
            String ip = getClass().getAnnotation(Replicated.class).targetIp();
            String port = getClass().getAnnotation(Replicated.class).targetPort();
            String user = getClass().getAnnotation(Replicated.class).targetUser();
            String password = getClass().getAnnotation(Replicated.class).targetPassword();
            String remoteDb = getClass().getAnnotation(Replicated.class).targetDbName();
            String selector = getClass().getAnnotation(Replicated.class).selector();
            
            if (!enabled.isBlank() && enabled.equalsIgnoreCase("true")) {
                ip = resolve(ip, false);
                port = resolve(port, false);
                user = resolve(user, true);
                password = resolve(password, true);
                remoteDb = resolve(remoteDb, true);
                selector = resolve(selector, true);
                
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
                
                logger.info("Starting replication to remote server: " + remoteServer);
                
                Map<String, Object> selectorMap = null;
                
                if (!selector.isBlank()) {
                    try {
                        selectorMap = mapper.readValue(selector, new TypeReference<Map<String, Object>>(){/*empty*/});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                CouchDbReplicationDocument toRemote = new CouchDbReplicationDocument(getDbName() + "$" + remoteDb + "_to", localServer, remoteServer, selectorMap);
                CouchDbReplicationDocument fromRemote = new CouchDbReplicationDocument(getDbName() + "$" + remoteDb + "_from", remoteServer, localServer, selectorMap);

                newReplicationDocs.put(toRemote.getDocId(), toRemote);
                newReplicationDocs.put(fromRemote.getDocId(), fromRemote);
            } else {
                logger.warn("Replication disabled");
            }
        }
        
        CouchDbConfig config = new CouchDbConfig.Builder().setHttpClient(getHttpClient())
                                                          .setIp(ip)
                                                          .setPort(port)
                                                          .setUser(user)
                                                          .setPassword(password)
                                                          .build();
        
        ReplicatorDb replicatorDb = new ReplicatorDb(config);
        
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
                    logger.info("Replication " + d.getSource() + " -> " + d.getTarget() + ": [OK]");
                } else {
                    logger.info("Replication " + d.getSource() + " -> " + d.getTarget() + ": [" + d.getConflictReason() + "]");
                }
            }
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
}