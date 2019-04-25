package com.n1global.acc;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.json.CouchDbBooleanResponse;
import com.n1global.acc.json.CouchDbDesignDocument;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbDocumentAccessor;
import com.n1global.acc.json.CouchDbInfo;
import com.n1global.acc.json.CouchDbPutResponse;
import com.n1global.acc.transformer.CouchDbBooleanResponseTransformer;
import com.n1global.acc.util.FutureUtils;
import com.n1global.acc.util.NoopFunction;
import com.n1global.acc.util.UrlBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class CouchDbAsyncOperations {
    private CouchDb couchDb;

    private AsyncHttpClient httpClient;

    public CouchDbAsyncOperations(CouchDb couchDb) {
        this.couchDb = couchDb;
        this.httpClient = couchDb.config.getHttpClient();
    }

    private UrlBuilder createUrlBuilder() {
        return new UrlBuilder(couchDb.getDbUrl());
    }

    //------------------ Save API -------------------------

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     *
     * @param batch If batch param is true then revision number will not be returned and
     * also CouchDB will silently reject update if document with same id already exists.
     * Use this option with caution.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> saveOrUpdate(final T doc, boolean batch) {
        return _saveOrUpdate(doc, batch);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> saveOrUpdate(final T doc) {
        return saveOrUpdate(doc, false);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     *
     * @param batch If batch param is true then revision number will not be returned and
     * also CouchDB will silently reject update if document with same id already exists.
     * Use this option with caution.
     */
    public CompletableFuture<Map<String, Object>> saveOrUpdate(final Map<String, Object> doc, boolean batch) {
        return _saveOrUpdate(doc, batch);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public CompletableFuture<Map<String, Object>> saveOrUpdate(final Map<String, Object> doc) {
        return saveOrUpdate(doc, false);
    }

    private <T> CompletableFuture<T> _saveOrUpdate(final T doc, boolean batch) {
        try {
            UrlBuilder urlBuilder = createUrlBuilder();

            if (batch) urlBuilder.addQueryParam("batch", "ok");

            Function<CouchDbPutResponse, T> transformer = resp -> {
                if (doc instanceof CouchDbDocument) {
                    CouchDbDocument couchDoc = (CouchDbDocument) doc;

                    couchDoc.setDocId(resp.getDocId());
                    couchDoc.setRev(resp.getRev());

                    new CouchDbDocumentAccessor(couchDoc).setCurrentDb(couchDb);
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) doc;

                    m.put("_id", resp.getDocId());
                    m.put("_rev", resp.getRev());
                }

                return doc;
            };
            
            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setBody(couchDb.mapper.writeValueAsString(doc))
                                                       .setUrl(urlBuilder.build())
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbPutResponse>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or delete multiple documents in to the database in a single request.
     *
     * @param docs
     * @param callback
     * @return
     */
    @SafeVarargs
    public final <T extends CouchDbDocument> CompletableFuture<T[]> saveOrUpdate(final T... docs) {
        try {
            ArrayNode arrayNode = couchDb.mapper.createArrayNode();

            for (T doc : docs) {
                arrayNode.addPOJO(doc);
            }

            ObjectNode objectNode = couchDb.mapper.createObjectNode();

            objectNode.set("docs", arrayNode);

            StringWriter writer = new StringWriter();

            couchDb.mapper.writeTree(couchDb.mapper.getFactory().createGenerator(writer), objectNode);

            Function<List<CouchDbPutResponse>, T[]> transformer = responses -> {
                for (int i = 0; i < docs.length; i++) {
                    docs[i].setDocId(responses.get(i).getDocId());
                    docs[i].setRev(responses.get(i).getRev());

                    new CouchDbDocumentAccessor(docs[i]).setCurrentDb(couchDb)
                                                        .setInConflict(responses.get(i).isInConflict())
                                                        .setForbidden(responses.get(i).isForbidden())
                                                        .setConflictReason(responses.get(i).getConflictReason());
                }

                return docs;
            };

            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                                                       .setBody(writer.toString())
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     *
     * @param docs
     * @param callback
     * @return
     */
    public <T extends CouchDbDocument> CompletableFuture<List<T>> saveOrUpdate(final List<T> docs) {
        try {
            ArrayNode arrayNode = couchDb.mapper.createArrayNode();

            for (T doc : docs) {
                arrayNode.addPOJO(doc);
            }

            ObjectNode objectNode = couchDb.mapper.createObjectNode();

            objectNode.set("docs", arrayNode);

            StringWriter writer = new StringWriter();

            couchDb.mapper.writeTree(couchDb.mapper.getFactory().createGenerator(writer), objectNode);

            Function<List<CouchDbPutResponse>, List<T>> transformer = responses -> {
                for (int i = 0; i < docs.size(); i++) {
                    docs.get(i).setDocId(responses.get(i).getDocId());
                    docs.get(i).setRev(responses.get(i).getRev());

                    new CouchDbDocumentAccessor(docs.get(i)).setCurrentDb(couchDb)
                                                            .setInConflict(responses.get(i).isInConflict())
                                                            .setForbidden(responses.get(i).isForbidden())
                                                            .setConflictReason(responses.get(i).getConflictReason());
                }

                return docs;
            };

            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                                                       .setBody(writer.toString())
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Insert or delete multiple documents in to the database in a single request.
     *
     * @param docs
     * @param callback
     * @return
     */
    @SafeVarargs
    public final CompletableFuture<List<CouchDbPutResponse>> saveOrUpdate(final Map<String, Object>... docs) {
        try {
            Map<String, Object> map = new HashMap<>();

            map.put("docs", docs);

            Function<List<CouchDbPutResponse>, List<CouchDbPutResponse>> transformer = responses -> {
                for (int i = 0; i < docs.length; i++) {
                    docs[i].put("_id", responses.get(i).getDocId());
                    docs[i].put("_rev", responses.get(i).getRev());
                }

                return responses;
            };

            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                                                       .setBody(couchDb.mapper.writeValueAsString(map))
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public CompletableFuture<CouchDbPutResponse> attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addPathSegment(name);

        if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
            urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
        }

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("PUT")
                                                   .setHeader("Content-Type", contentType)
                                                   .setBody(in)
                                                   .setUrl(urlBuilder.build())
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbPutResponse>() {/* empty */}, new NoopFunction<CouchDbPutResponse>(), couchDb.mapper)));
    }

    /**
     * Attach content to the document.
     */
    public CompletableFuture<CouchDbPutResponse> attach(CouchDbDocument doc, InputStream in, String name, String contentType) {
        return attach(doc.getDocIdAndRev(), in, name, contentType);
    }

    /**
     * Attach content to non-existing document.
     */
    public CompletableFuture<CouchDbPutResponse> attach(String docId, InputStream in, String name, String contentType) {
        return attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }

    /**
     * Gets an attachment of the document.
     */
    public CompletableFuture<Response> getAttachment(String docId, String name) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("GET")
                                                   .setUrl(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build())
                                                   .execute());
    }

    /**
     * Gets an attachment of the document.
     */
    public CompletableFuture<Response> getAttachment(CouchDbDocument doc, String name) {
        return getAttachment(doc.getDocId(), name);
    }

    /**
     * Deletes an attachment from the document.
     */
    public CompletableFuture<Boolean> deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId())
                                                  .addPathSegment(name)
                                                  .addQueryParam("rev", docIdAndRev.getRev());

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("DELETE")
                                                   .setUrl(urlBuilder.build())
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }

    /**
     * Deletes an attachment from the document.
     */
    public CompletableFuture<Boolean> deleteAttachment(CouchDbDocument doc, String name) {
        return deleteAttachment(doc.getDocIdAndRev(), name);
    }

    //------------------ Fetch API -------------------------

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    private <T> CompletableFuture<T> get(CouchDbDocIdAndRev docIdAndRev, JavaType docType, boolean revsInfo) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addQueryParam("revs_info", Boolean.toString(revsInfo));

        if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
            urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
        }

        Function<T, T> transformer = doc -> {
            if (doc != null && doc instanceof CouchDbDocument) {
                new CouchDbDocumentAccessor((CouchDbDocument) doc).setCurrentDb(couchDb);
            }

            return doc;
        };

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("GET")
                                                   .setUrl(urlBuilder.build())
                                                   .execute(new CouchDbAsyncHandler<>(docType, transformer, couchDb.mapper)));
    }

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> get(CouchDbDocIdAndRev docIdAndRev, boolean revsInfo) {
        return get(docIdAndRev, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), revsInfo);
    }

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> get(CouchDbDocIdAndRev docIdAndRev) {
        return get(docIdAndRev, false);
    }

    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> get(String docId) {
        return get(new CouchDbDocIdAndRev(docId, null));
    }

    /**
     * Returns the latest revision of the document.
     */
    public CompletableFuture<Map<String, Object>> getRaw(String docId) {
        return get(new CouchDbDocIdAndRev(docId, null), TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), false);
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
    public CompletableFuture<Boolean> delete(CouchDbDocIdAndRev docIdAndRev) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("DELETE")
                                                   .setUrl(createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addQueryParam("rev", docIdAndRev.getRev()).build())
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
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
    public CompletableFuture<Boolean> delete(CouchDbDocument doc) {
        return delete(doc.getDocIdAndRev());
    }

    //------------------ Admin API -------------------------

    public CompletableFuture<List<CouchDbDesignDocument>> getDesignDocs() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").async().asDocs();
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public CompletableFuture<List<String>> getAllDbsAsync() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().addPathSegment("/_all_dbs").build())
                                                   .setMethod("GET")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<List<String>>() {/* empty */}, new NoopFunction<List<String>>(), couchDb.mapper)));
    }

    /**
     * Create a new database.
     */
    public CompletableFuture<Boolean> createDb() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().build())
                                                   .setMethod("PUT")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, resp -> resp.isOk(), couchDb.mapper)));
    }

    /**
     * Delete an existing database.
     */
    public CompletableFuture<Boolean> deleteDb() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().build())
                                                   .setMethod("DELETE")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }

    /**
     * Returns database information.
     */
    public CompletableFuture<CouchDbInfo> getInfo() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().build())
                                                   .setMethod("GET")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbInfo>() {/* empty */}, new NoopFunction<CouchDbInfo>(), couchDb.mapper)));
    }

    /**
     * Compaction compresses the database file by removing unused sections created during updates.
     * Old revisions of documents are also removed from the database though a small amount of meta
     * data is kept for use in conflict during replication.
     */
    public CompletableFuture<Boolean> compact() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().addPathSegment("_compact").build())
                                                   .setMethod("POST")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }

    /**
     * Starts a compaction for all the views in the selected design document, requires admin privileges.
     */
    public CompletableFuture<Boolean> compactViews(String designName) {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().addPathSegment("_compact").addPathSegment(designName).build())
                                                   .setMethod("POST")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }

    /**
     * Removes view files that are not used by any design document, requires admin privileges.
     *
     * View indexes on disk are named after their MD5 hash of the view definition.
     * When you change a view, old indexes remain on disk. To clean up all outdated view indexes
     * (files named after the MD5 representation of views, that does not exist anymore) you can
     * trigger a view cleanup.
     */
    public CompletableFuture<Boolean> cleanupViews() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().addPathSegment("_view_cleanup").build())
                                                   .setMethod("POST")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }

    /**
     * Makes sure all uncommited changes are written and synchronized to the disk.
     */
    public CompletableFuture<Boolean> ensureFullCommit() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(createUrlBuilder().addPathSegment("_ensure_full_commit").build())
                                                   .setMethod("POST")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper)));
    }
}
