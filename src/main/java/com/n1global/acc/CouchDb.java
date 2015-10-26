package com.n1global.acc;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.annotation.DbName;
import com.n1global.acc.annotation.Filter;
import com.n1global.acc.annotation.IgnorePrefix;
import com.n1global.acc.annotation.JsView;
import com.n1global.acc.annotation.Security;
import com.n1global.acc.annotation.SecurityPattern;
import com.n1global.acc.annotation.UpdateHandler;
import com.n1global.acc.annotation.ValidateDocUpdate;
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
import com.ning.http.client.Response;

public class CouchDb extends CouchDbBase {
    private String dbName;

    private CouchDbBuiltInView builtInView;

    /**
     * For async query processing.
     */
    private CouchDbAsyncOperations asyncOps = new CouchDbAsyncOperations(this);

    public CouchDb(CouchDbConfig config) {
        super(config);
        
        testConnection();

        generateDbName();

        createDbIfNotExist();
        
        injectBuiltInView();
        
        if (config.isSelfDiscovering()) {
            selfDiscovering();
        }
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUrl() {
        return new UrlBuilder(getConfig().getServerUrl()).addPathSegment(getDbName()).toString();
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
    public Map<String, Object> saveOrUpdateRaw(Map<String, Object> doc, boolean batch) {
        return ExceptionHandler.handleFutureResult(asyncOps.saveOrUpdateRaw(doc, batch));
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public Map<String, Object> saveOrUpdateRaw(Map<String, Object> doc) {
        return saveOrUpdateRaw(doc, false);
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

    //------------------ Bulk API -------------------------

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> T[] bulk( @SuppressWarnings("unchecked") T... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.bulk(docs));
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> List<T> bulk(List<T> docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.bulk(docs));
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    @SafeVarargs
    final public List<CouchDbPutResponse> bulkRaw(Map<String, Object>... docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.bulkRaw(docs));
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     */
    public List<CouchDbPutResponse> bulkRaw(List<Map<String, Object>> docs) {
        return ExceptionHandler.handleFutureResult(asyncOps.bulkRaw(docs));
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return ExceptionHandler.handleFutureResult(asyncOps.getDesignDocs());
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

        injectDirectUpdaters();

        injectFilters();
        
        injectUpdaters();
        
        addSecurity();

        compactAllOnStart();
    }

    private void testConnection() {
        try {
            if (config.getHttpClient().prepareRequest(prototype)
                                      .setMethod("GET")
                                      .setUrl(getConfig().getServerUrl())
                                      .execute().get().getStatusCode() != 200) {
                throw new ConnectException("Could not connect to " + getConfig().getServerUrl());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateDbName() {
        dbName = getConfig().getDbName();

        if (dbName == null) {
            if (getClass().isAnnotationPresent(DbName.class)) {
                dbName = getClass().getAnnotation(DbName.class).value();
            } else {
                dbName = NamedStrategy.addUnderscores(getClass().getSimpleName());
            }
        }
        
        if (!getClass().isAnnotationPresent(IgnorePrefix.class)) {
            dbName = getConfig().getDbPrefix() + dbName;
        }
    }

    private void createDbIfNotExist() {
        CouchDbAdmin admin = new CouchDbAdmin(getConfig());

        if (!admin.getListDbs().contains(getDbName())) {
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

                    if (((CouchDbConfig)getConfig()).isBuildViewsOnStart()) {
                        view.createQuery().byKey("123").asKey();
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbReduceView.class) {
                    CouchDbReduceView<String, Object> view = new CouchDbReduceView<>(this, designName, viewName, jts);

                    if (((CouchDbConfig)getConfig()).isBuildViewsOnStart()) {
                        view.createQuery().byKey("123").asKey();
                    }

                    injectedView = view;
                }

                if (viewClass == CouchDbMapReduceView.class) {
                    CouchDbMapReduceView<String, Object, Object, Object> view = new CouchDbMapReduceView<>(this, designName, viewName, jts);

                    if (((CouchDbConfig)getConfig()).isBuildViewsOnStart()) {
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

    private void injectDirectUpdaters() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(UpdateHandler.class)) {
                UpdateHandler handler = field.getAnnotation(UpdateHandler.class);

                String handlerName = handler.handlerName().isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : handler.handlerName();

                setValue(field, new CouchDbDirectUpdater(this, handlerName, handler.designName()));
            }
        }
    }

    private void injectFilters() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(Filter.class)) {
                Filter filter = field.getAnnotation(Filter.class);

                String filterName = filter.filterName().isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : filter.filterName();

                setValue(field, new CouchDbFilter(filter.designName(), filterName));
            }
        }
    }
    
    private void injectUpdaters() {
        for (Field field : ReflectionUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(ValidateDocUpdate.class)) {
                ValidateDocUpdate vdu = field.getAnnotation(ValidateDocUpdate.class);

                setValue(field, new CouchDbValidator(vdu.predicate()));
            }
        }
    }
    
    private void addSecurity() {
        try {
            if (getClass().isAnnotationPresent(Security.class)) {
                SecurityPattern adminsPattern = getClass().getAnnotation(Security.class).admins();
                SecurityPattern membersPattern = getClass().getAnnotation(Security.class).members();
                
                CouchDbSecurityObject securityObject = new CouchDbSecurityObject();
                
                securityObject.setAdmins(new CouchDbSecurityPattern(adminsPattern.names(), adminsPattern.roles()));
                securityObject.setMembers(new CouchDbSecurityPattern(membersPattern.names(), membersPattern.roles()));
                
                Response r = getConfig().getHttpClient().prepareRequest(prototype).setMethod("PUT")
                                                                                  .setUrl(new UrlBuilder(getDbUrl()).addPathSegment("_security").build())
                                                                                  .setBody(mapper.writeValueAsString(securityObject))
                                                                                  .execute().get();
                if (r.getStatusCode() != 200) throw new RuntimeException("Can't apply security");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void compactAllOnStart() {
        if (((CouchDbConfig)config).isCompactAllOnStart()) {
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
            bulk(new ArrayList<>(newDesignDocs.values()));
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

            if (field.isAnnotationPresent(Filter.class)) {
                field.setAccessible(true);

                Filter filter = field.getAnnotation(Filter.class);

                String designName = "_design/" + filter.designName();
                String filterName = filter.filterName().isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : filter.filterName();

                designMap.putIfAbsent(designName, new CouchDbDesignDocument(designName));
                designMap.get(designName).addFilter(filterName, "function(doc, req) { " + filter.predicate() + ";}");
            }
            
            if (field.isAnnotationPresent(ValidateDocUpdate.class)) {
                field.setAccessible(true);

                ValidateDocUpdate vdu = field.getAnnotation(ValidateDocUpdate.class);

                String designName = "_design/" + vdu.designName();

                designMap.putIfAbsent(designName, new CouchDbDesignDocument(designName));
                designMap.get(designName).setValidateDocUpdate("function(newDoc, oldDoc, userCtx, secObj) { " + vdu.predicate() + ";}");
            }

            if (field.isAnnotationPresent(UpdateHandler.class)) {
                field.setAccessible(true);

                UpdateHandler handler = field.getAnnotation(UpdateHandler.class);

                String designName = "_design/" + handler.designName();
                String handlerName = handler.handlerName().isEmpty() ? NamedStrategy.addUnderscores(field.getName()) : handler.handlerName();

                designMap.putIfAbsent(designName, new CouchDbDesignDocument(designName));
                designMap.get(designName).addUpdateHandler(handlerName, "function(doc, req) { " + handler.func() + ";}");
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