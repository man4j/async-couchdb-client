package com.n1global.acc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.n1global.acc.annotation.DbName;
import com.n1global.acc.annotation.IgnorePrefix;
import com.n1global.acc.annotation.JsView;
import com.n1global.acc.annotation.Security;
import com.n1global.acc.annotation.SecurityPattern;
import com.n1global.acc.json.CouchDbDesignDocument;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbInfo;
import com.n1global.acc.json.CouchDbPutResponse;
import com.n1global.acc.json.security.CouchDbSecurityObject;
import com.n1global.acc.json.security.CouchDbSecurityPattern;
import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.NamedStrategy;
import com.n1global.acc.util.ReflectionUtils;
import com.n1global.acc.util.UrlBuilder;
import com.n1global.acc.view.CouchDbBuiltInView;
import com.n1global.acc.view.CouchDbMapReduceView;
import com.n1global.acc.view.CouchDbMapView;
import com.n1global.acc.view.CouchDbReduceView;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.Realm.AuthScheme;

public class CouchDb {
    final CouchDbConfig config;

    final ObjectMapper mapper = new ObjectMapper();

    final Request prototype;
    
    private String dbName;

    private CouchDbBuiltInView builtInView;

    /**
     * For async query processing.
     */
    private CouchDbAsyncOperations asyncOps = new CouchDbAsyncOperations(this);

    public CouchDb(CouchDbConfig config) {
        mapper.registerModule(new JavaTimeModule());
        
        this.config = config;

        RequestBuilder builder = new RequestBuilder().setHeader("Content-Type", "application/json; charset=utf-8")
                                                     .setBodyEncoding("UTF-8");

        if (this.config.getUser() != null && this.config.getPassword() != null) {
            Realm realm = new Realm.RealmBuilder()
                                   .setPrincipal(this.config.getUser())
                                   .setPassword(this.config.getPassword())
                                   .setUsePreemptiveAuth(true)
                                   .setScheme(AuthScheme.BASIC)
                                   .build();

            builder.setRealm(realm);
        }

        prototype = builder.build();
        
        testConnection();

        generateDbName();

        createDbIfNotExist();
        
        injectBuiltInView();
        
        if (config.isSelfDiscovering()) {
            selfDiscovering();
        }
    }
    
    public CouchDbConfig getConfig() {
        return config;
    }
    
    public ObjectMapper getMapper() {
        return mapper;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUrl() {
        return new UrlBuilder(config.getServerUrl()).addPathSegment(getDbName()).toString();
    }

    public CouchDbBuiltInView getBuiltInView() {
        return builtInView;
    }

    public CouchDbAsyncOperations async() {
        return asyncOps;
    }

    //------------------ Save API -------------------------

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     *
     * @param batch If batch param is true then revision number will not be returned and
     * also CouchDB will silently reject update if document with same id already exists.
     * Use this option with caution.
     */
    public <T extends CouchDbDocument> T saveOrUpdate(T doc, boolean batch) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(doc, batch));
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public <T extends CouchDbDocument> T saveOrUpdate(T doc) {
        return saveOrUpdate(doc, false);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     *
     * @param batch If batch param is true then revision number will not be returned and
     * also CouchDB will silently reject update if document with same id already exists.
     * Use this option with caution.
     */
    public Map<String, Object> saveOrUpdate(Map<String, Object> doc, boolean batch) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(doc, batch));
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public Map<String, Object> saveOrUpdate(Map<String, Object> doc) {
        return saveOrUpdate(doc, false);
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    @SafeVarargs
    public final <T extends CouchDbDocument> T[] saveOrUpdate(T... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    @SafeVarargs
    public final List<CouchDbPutResponse> saveOrUpdate(Map<String, Object>... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdate(docs));
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public CouchDbPutResponse attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        return ExceptionHandler.handleFutureResult(asyncOps.attach(docIdAndRev, in, name, contentType));
    }

    /**
     * Attach content to the document.
     */
    public CouchDbPutResponse attach(CouchDbDocument doc, InputStream in, String name, String contentType) {
        return attach(doc.getDocIdAndRev(), in, name, contentType);
    }

    /**
     * Attach content to non-existing document.
     */
    public CouchDbPutResponse attach(String docId, InputStream in, String name, String contentType) {
        return attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }

    /**
     * Gets an attachment of the document.
     */
    public Response getAttachment(String docId, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.getAttachment(docId, name));
    }

    /**
     * Gets an attachment of the document.
     */
    public Response getAttachment(CouchDbDocument doc, String name) {
        return getAttachment(doc.getDocId(), name);
    }

    /**
     * Deletes an attachment from the document.
     */
    public boolean deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.deleteAttachment(docIdAndRev, name));
    }

    /**
     * Deletes an attachment from the document.
     */
    public boolean deleteAttachment(CouchDbDocument doc, String name) {
        return deleteAttachment(doc.getDocIdAndRev(), name);
    }

    //------------------ Fetch API -------------------------

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    public <T extends CouchDbDocument> T get(CouchDbDocIdAndRev docIdAndRev, boolean revsInfo) {
        return ExceptionHandler.handleFutureResult(asyncOps.get(docIdAndRev, revsInfo));
    }

    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(CouchDbDocIdAndRev docIdAndRev) {
        return get(docIdAndRev, false);
    }

    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> T get(String docId) {
        return get(new CouchDbDocIdAndRev(docId, null));
    }

    /**
     * Returns the latest revision of the document.
     */
    public Map<String, Object> getRaw(String docId) {
        return ExceptionHandler.handleFutureResult(asyncOps.getRaw(docId));
    }

    //------------------ Delete API -------------------------

    /**
     * Deletes the document.
     *
     * When you delete a document the database will create a new revision which contains the _id and _rev fields
     * as well as a deleted flag. This revision will remain even after a database compaction so that the deletion
     * can be replicated. Deleted documents, like non-deleted documents, can affect view build times, PUT and
     * DELETE requests time and size of database on disk, since they increase the size of the B+Tree's. You can
     * see the number of deleted documents in database {@link com.n1global.acc.CouchDb#getInfo() information}.
     * If your use case creates lots of deleted documents (for example, if you are storing short-term data like
     * logfile entries, message queues, etc), you might want to periodically switch to a new database and delete
     * the old one (once the entries in it have all expired).
     */
    public boolean delete(CouchDbDocument doc) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(doc));
    }

    /**
     * Deletes the document.
     *
     * When you delete a document the database will create a new revision which contains the _id and _rev fields
     * as well as a deleted flag. This revision will remain even after a database compaction so that the deletion
     * can be replicated. Deleted documents, like non-deleted documents, can affect view build times, PUT and
     * DELETE requests time and size of database on disk, since they increase the size of the B+Tree's. You can
     * see the number of deleted documents in database {@link com.n1global.acc.CouchDb#getInfo() information}.
     * If your use case creates lots of deleted documents (for example, if you are storing short-term data like
     * logfile entries, message queues, etc), you might want to periodically switch to a new database and delete
     * the old one (once the entries in it have all expired).
     */
    public boolean delete(CouchDbDocIdAndRev docId) {
        return ExceptionHandler.handleFutureResult(asyncOps.delete(docId));
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return ExceptionHandler.handleFutureResult(asyncOps.getDesignDocs());
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public List<String> getAllDbs() {
        return ExceptionHandler.handleFutureResult(asyncOps.getAllDbsAsync());
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
     * Compaction compresses the database file by removing unused sections created during updates.
     * Old revisions of documents are also removed from the database though a small amount of meta
     * data is kept for use in conflict during replication.
     */
    public boolean compact() {
        return ExceptionHandler.handleFutureResult(asyncOps.compact());
    }

    /**
     * Starts a compaction for all the views in the selected design document, requires admin privileges.
     */
    public boolean compactViews(String designName) {
        return ExceptionHandler.handleFutureResult(asyncOps.compactViews(designName));
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

    /**
     * Makes sure all uncommited changes are written and synchronized to the disk.
     */
    public boolean ensureFullCommit() {
        return ExceptionHandler.handleFutureResult(asyncOps.ensureFullCommit());
    }

    //------------------ Discovering methods -------------------------

    private void selfDiscovering() {
        synchronizeDesignDocs();

        injectViews();
                
        addSecurity();

        compactAllOnStart();
    }

    private void testConnection() {
        try {
            if (config.getHttpClient().prepareRequest(prototype)
                                      .setMethod("GET")
                                      .setUrl(config.getServerUrl())
                                      .execute().get().getStatusCode() != 200) {
                throw new ConnectException("Could not connect to " + config.getServerUrl());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateDbName() {
        dbName = config.getDbName();

        if (dbName == null) {
            if (getClass().isAnnotationPresent(DbName.class)) {
                dbName = getClass().getAnnotation(DbName.class).value();
            } else {
                dbName = NamedStrategy.addUnderscores(getClass().getSimpleName());
            }
        }
        
        if (!getClass().isAnnotationPresent(IgnorePrefix.class)) {
            dbName = config.getDbPrefix() + dbName;
        }
    }

    private void createDbIfNotExist() {
        if (!getAllDbs().contains(getDbName())) {
            createDb();
        }
    }

    private void injectViews() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            String designName = null, viewName = null;

            if (field.isAnnotationPresent(JsView.class)) {
                JsView view = field.getAnnotation(JsView.class);

                designName = view.designName();
                viewName = view.viewName();
            }

            if (designName != null && viewName != null) {
                viewName = viewName.isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : viewName;

                Class<?> viewClass = field.getType();

                TypeFactory tf = TypeFactory.defaultInstance();

                JavaType[] jts = tf.findTypeParameters(tf.constructType(field.getGenericType()), viewClass);

                Object injectedView = null;

                if (viewClass == CouchDbMapView.class) {
                    CouchDbMapView<String, Object> view = new CouchDbMapView<>(this, designName, viewName, jts);

                    if (config.isBuildViewsOnStart()) {
                        view.createQuery().byKey("123").asKey();
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbReduceView.class) {
                    CouchDbReduceView<String, Object> view = new CouchDbReduceView<>(this, designName, viewName, jts);

                    if (config.isBuildViewsOnStart()) {
                        view.createQuery().byKey("123").asKey();
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbMapReduceView.class) {
                    CouchDbMapReduceView<String, Object, Object, Object> view = new CouchDbMapReduceView<>(this, designName, viewName, jts);

                    if (config.isBuildViewsOnStart()) {
                        view.createMapQuery().byKey("123").asKey();
                    }

                    injectedView = view;
                }

                if (injectedView != null) {
                    setValue(field, injectedView);
                } else {
                    throw new IllegalStateException("Invalid view class");
                }
            }
        }
    }

    private void injectBuiltInView() {
        builtInView = new CouchDbBuiltInView(this);
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
                                                                .getResponseBody("UTF-8"), CouchDbSecurityObject.class);
    }
    
    private void compactAllOnStart() {
        if (config.isCompactAllOnStart()) {
            compact();
            cleanupViews();

            for (CouchDbDesignDocument d : getDesignDocs()) compactViews(d.getDocId().substring(d.getDocId().indexOf("/") + 1));
        }
    }

    private void synchronizeDesignDocs() {
        List<CouchDbDesignDocument> oldDesignDocs = getDesignDocs();

        Map<String, CouchDbDesignDocument> newDesignDocs = generateNewDesignDocs();

        for (CouchDbDesignDocument oldDoc : oldDesignDocs) {
            if (!newDesignDocs.containsKey(oldDoc.getDocId())) {
                delete(oldDoc);
            } else {
                CouchDbDesignDocument newDoc = newDesignDocs.get(oldDoc.getDocId());

                if (newDoc.equals(oldDoc)) {
                    newDesignDocs.remove(oldDoc.getDocId());
                } else {
                    delete(oldDoc);
                }
            }
        }

        if (!newDesignDocs.isEmpty()) {
            saveOrUpdate(new ArrayList<>(newDesignDocs.values()));
        }
    }

    private Map<String, CouchDbDesignDocument> generateNewDesignDocs() {
        Map<String, CouchDbDesignDocument> designMap = new HashMap<>();

        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(JsView.class)) {
                JsView view = field.getAnnotation(JsView.class);

                String designName = "_design/" + view.designName();
                String viewName = view.viewName().isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : view.viewName();

                String map = "function(doc) {" + view.map() + ";}";

                String reduce = null;

                if (!view.reduce().isEmpty()) {
                    if (Arrays.asList(JsView.COUNT, JsView.STATS, JsView.SUM).contains(view.reduce())) {
                        reduce = view.reduce();
                    } else {
                        reduce = "function(key, values, rereduce) {" + view.reduce() + ";}";
                    }
                }

                designMap.putIfAbsent(designName, new CouchDbDesignDocument(designName));
                designMap.get(designName).addView(viewName, map, reduce);
            }
        }

        return designMap;
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