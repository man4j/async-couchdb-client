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
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import com.equiron.acc.annotation.DbName;
import com.equiron.acc.annotation.IgnorePrefix;
import com.equiron.acc.annotation.JsView;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbInfo;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CouchDb {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

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
                                                     .setCharset(StandardCharsets.UTF_8);

        if (this.config.getUser() != null && this.config.getPassword() != null) {
            Realm realm = new Realm.Builder(this.config.getUser(), this.config.getPassword())
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
    
    public Request getPrototype() {
        return prototype;
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
    public final Map<String, Object> purge(final CouchDbDocIdAndRev docRev, final CouchDbDocIdAndRev... docRevs) {
        return ExceptionHandler.handleFutureResult(asyncOps.purge(docRev, docRevs));
    }
    
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public Map<String, Object> purge(final List<CouchDbDocIdAndRev> docRevs) {
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
     * Gets an attachment of the document.
     */
    public Response getAttachment(String docId, String name) {
        return ExceptionHandler.handleFutureResult(asyncOps.getAttachment(docId, name));
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

    private void selfDiscovering() {
        synchronizeDesignDocs();

        injectViews();
                
        addSecurity();

        cleanupViews();
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
        if (!getDatabases().contains(getDbName())) {
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
                        long t = System.currentTimeMillis();
                        logger.info("Building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName + "...");
                        view.createQuery().byKey("123").asKey();
                        logger.info("Complete building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " seconds");
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbReduceView.class) {
                    CouchDbReduceView<String, Object> view = new CouchDbReduceView<>(this, designName, viewName, jts);

                    if (config.isBuildViewsOnStart()) {
                        long t = System.currentTimeMillis();
                        logger.info("Building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName + "...");
                        view.createQuery().byKey("123").asKey();
                        logger.info("Complete building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " seconds");
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbMapReduceView.class) {
                    CouchDbMapReduceView<String, Object, Object, Object> view = new CouchDbMapReduceView<>(this, designName, viewName, jts);

                    if (config.isBuildViewsOnStart()) {
                        long t = System.currentTimeMillis();
                        logger.info("Building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName + "...");
                        view.createMapQuery().byKey("123").asKey();
                        logger.info("Complete building view in database: " + config.getDbName() + ". View name: " + designName + "/" + viewName  + " in " + (System.currentTimeMillis() - t) + " seconds");                        
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
                                                                .getResponseBody(StandardCharsets.UTF_8), CouchDbSecurityObject.class);
    }
    
    private void synchronizeDesignDocs() {
        List<CouchDbDesignDocument> oldDesignDocs = getDesignDocs();

        Map<String, CouchDbDesignDocument> newDesignDocs = generateNewDesignDocs();

        for (CouchDbDesignDocument oldDoc : oldDesignDocs) {
            if (!newDesignDocs.containsKey(oldDoc.getDocId())) {
                delete(oldDoc.getDocIdAndRev());
            } else {
                CouchDbDesignDocument newDoc = newDesignDocs.get(oldDoc.getDocId());

                if (newDoc.equals(oldDoc)) {
                    newDesignDocs.remove(oldDoc.getDocId());
                } else {
                    delete(oldDoc.getDocIdAndRev());
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