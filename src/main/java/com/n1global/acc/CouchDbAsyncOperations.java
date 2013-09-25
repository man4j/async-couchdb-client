package com.n1global.acc;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.n1global.acc.util.Function;
import com.n1global.acc.util.NoopFunction;
import com.n1global.acc.util.UrlBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class CouchDbAsyncOperations {
    private CouchDb couchDb;

    private AsyncHttpClient httpClient;

    public CouchDbAsyncOperations(CouchDb couchDb) {
        this.couchDb = couchDb;
        this.httpClient = couchDb.getConfig().getHttpClient();
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
    public <T extends CouchDbDocument> ListenableFuture<T> saveOrUpdate(final T doc, boolean batch) {
        return _saveOrUpdate(doc, batch);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public <T extends CouchDbDocument> ListenableFuture<T> saveOrUpdate(final T doc) {
        return saveOrUpdate(doc, false);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     *
     * @param batch If batch param is true then revision number will not be returned and
     * also CouchDB will silently reject update if document with same id already exists.
     * Use this option with caution.
     */
    public ListenableFuture<Map<String, Object>> saveOrUpdateRaw(final Map<String, Object> doc, boolean batch) {
        return _saveOrUpdate(doc, batch);
    }

    /**
     * Inserts a new document with an automatically generated id or inserts a new version of the document.
     */
    public ListenableFuture<Map<String, Object>> saveOrUpdateRaw(final Map<String, Object> doc) {
        return saveOrUpdateRaw(doc, false);
    }

    private <T> ListenableFuture<T> _saveOrUpdate(final T doc, boolean batch) {
        try {
            UrlBuilder urlBuilder = createUrlBuilder();

            if (batch) urlBuilder.addQueryParam("batch", "ok");

            Function<CouchDbPutResponse, T> transformer = new Function<CouchDbPutResponse, T>() {
                @Override
                public T apply(CouchDbPutResponse putResponse) {
                    if (doc instanceof CouchDbDocument) {
                        CouchDbDocument couchDoc = (CouchDbDocument) doc;

                        couchDoc.setDocId(putResponse.getDocId());
                        couchDoc.setRev(putResponse.getRev());

                        new CouchDbDocumentAccessor(couchDoc).setCurrentDb(couchDb);
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) doc;

                        m.put("_id", putResponse.getDocId());
                        m.put("_rev", putResponse.getRev());
                    }

                    return doc;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("POST")
                             .setBody(couchDb.mapper.writeValueAsString(doc))
                             .setUrl(urlBuilder.build())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbPutResponse>() {/* empty */}, transformer, couchDb.mapper));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public ListenableFuture<CouchDbPutResponse> attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        try {
            UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addPathSegment(name);

            if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
                urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
            }

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("PUT")
                             .setHeader("Content-Type", contentType)
                             .setBody(in)
                             .setUrl(urlBuilder.build())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbPutResponse>() {/* empty */}, new NoopFunction<CouchDbPutResponse>(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attach content to the document.
     */
    public ListenableFuture<CouchDbPutResponse> attach(CouchDbDocument doc, InputStream in, String name, String contentType) {
        return attach(doc.getDocIdAndRev(), in, name, contentType);
    }

    /**
     * Attach content to non-existing document.
     */
    public ListenableFuture<CouchDbPutResponse> attach(String docId, InputStream in, String name, String contentType) {
        return attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }

    /**
     * Gets an attachment of the document.
     */
    public ListenableFuture<Response> getAttachment(String docId, String name) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("GET")
                             .setUrl(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build())
                             .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets an attachment of the document.
     */
    public ListenableFuture<Response> getAttachment(CouchDbDocument doc, String name) {
        return getAttachment(doc.getDocId(), name);
    }

    /**
     * Deletes an attachment from the document.
     */
    public ListenableFuture<Boolean> deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        try {
            UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId())
                                                      .addPathSegment(name)
                                                      .addQueryParam("rev", docIdAndRev.getRev());

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("DELETE")
                             .setUrl(urlBuilder.build())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes an attachment from the document.
     */
    public ListenableFuture<Boolean> deleteAttachment(CouchDbDocument doc, String name) {
        return deleteAttachment(doc.getDocIdAndRev(), name);
    }

    //------------------ Fetch API -------------------------

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    private <T> ListenableFuture<T> get(CouchDbDocIdAndRev docIdAndRev, JavaType docType, boolean revsInfo) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        try {
            UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addQueryParam("revs_info", revsInfo + "");

            if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
                urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
            }

            Function<T, T> transformer = new Function<T, T>() {
                @Override
                public T apply(T document) {
                    if (document != null && document instanceof CouchDbDocument) {
                        new CouchDbDocumentAccessor((CouchDbDocument) document).setCurrentDb(couchDb);
                    }

                    return document;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("GET")
                             .setUrl(urlBuilder.build())
                             .execute(new CouchDbAsyncHandler<>(docType, transformer, couchDb.mapper));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    public <T extends CouchDbDocument> ListenableFuture<T> get(CouchDbDocIdAndRev docIdAndRev, boolean revsInfo) {
        return get(docIdAndRev, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), revsInfo);
    }

    /**
     * Returns the latest revision of the document if revision not specified.
     */
    public <T extends CouchDbDocument> ListenableFuture<T> get(CouchDbDocIdAndRev docIdAndRev) {
        return get(docIdAndRev, false);
    }

    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> ListenableFuture<T> get(String docId) {
        return get(new CouchDbDocIdAndRev(docId, null));
    }

    /**
     * Returns the latest revision of the document.
     */
    public ListenableFuture<Map<String, Object>> getRaw(String docId) {
        return get(new CouchDbDocIdAndRev(docId, null), TypeFactory.defaultInstance().constructParametricType(Map.class, String.class, Object.class), false);
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
    public ListenableFuture<Boolean> delete(CouchDbDocIdAndRev docIdAndRev) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("DELETE")
                             .setUrl(createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addQueryParam("rev", docIdAndRev.getRev()).build())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
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
    public ListenableFuture<Boolean> delete(CouchDbDocument doc) {
        return delete(doc.getDocIdAndRev());
    }

    //------------------ Bulk API -------------------------

    /**
     * Insert or delete multiple documents in to the database in a single request.
     *
     * @param docs
     * @param callback
     * @return
     */
    public <T extends CouchDbDocument> ListenableFuture<T[]> bulk(final T[] docs) {
        try {
            ArrayNode arrayNode = couchDb.mapper.createArrayNode();

            for (T doc : docs) {
                arrayNode.addPOJO(doc);
            }

            ObjectNode objectNode = couchDb.mapper.createObjectNode();

            objectNode.put("docs", arrayNode);

            StringWriter writer = new StringWriter();

            couchDb.mapper.writeTree(couchDb.mapper.getFactory().createGenerator(writer), objectNode);

            Function<List<CouchDbPutResponse>, T[]> transformer = new Function<List<CouchDbPutResponse>, T[]>() {
                @Override
                public T[] apply(List<CouchDbPutResponse> responses) {
                    for (int i = 0; i < docs.length; i++) {
                        docs[i].setDocId(responses.get(i).getDocId());
                        docs[i].setRev(responses.get(i).getRev());

                        new CouchDbDocumentAccessor(docs[i]).setCurrentDb(couchDb)
                                                            .setInConflict(responses.get(i).isInConflict())
                                                            .setForbidden(responses.get(i).isForbidden())
                                                            .setConflictReason(responses.get(i).getConflictReason());
                    }

                    return docs;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("POST")
                             .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                             .setBody(writer.toString())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper));
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
    public <T extends CouchDbDocument> ListenableFuture<List<T>> bulk(final List<T> docs) {
        try {
            ArrayNode arrayNode = couchDb.mapper.createArrayNode();

            for (T doc : docs) {
                arrayNode.addPOJO(doc);
            }

            ObjectNode objectNode = couchDb.mapper.createObjectNode();

            objectNode.put("docs", arrayNode);

            StringWriter writer = new StringWriter();

            couchDb.mapper.writeTree(couchDb.mapper.getFactory().createGenerator(writer), objectNode);

            Function<List<CouchDbPutResponse>, List<T>> transformer = new Function<List<CouchDbPutResponse>, List<T>>() {
                @Override
                public List<T> apply(List<CouchDbPutResponse> responses) {
                    for (int i = 0; i < docs.size(); i++) {
                        docs.get(i).setDocId(responses.get(i).getDocId());
                        docs.get(i).setRev(responses.get(i).getRev());

                        new CouchDbDocumentAccessor(docs.get(i)).setCurrentDb(couchDb)
                                                                .setInConflict(responses.get(i).isInConflict())
                                                                .setForbidden(responses.get(i).isForbidden())
                                                                .setConflictReason(responses.get(i).getConflictReason());
                    }

                    return docs;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("POST")
                             .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                             .setBody(writer.toString())
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper));
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
    public ListenableFuture<List<CouchDbPutResponse>> bulkRaw(final Map<String, Object>[] docs) {
        try {
            Map<String, Object> map = new HashMap<>();

            map.put("docs", docs);

            Function<List<CouchDbPutResponse>, List<CouchDbPutResponse>> transformer = new Function<List<CouchDbPutResponse>, List<CouchDbPutResponse>>() {
                @Override
                public List<CouchDbPutResponse> apply(List<CouchDbPutResponse> responses) {
                    for (int i = 0; i < docs.length; i++) {
                        docs[i].put("_id", responses.get(i).getDocId());
                        docs[i].put("_rev", responses.get(i).getRev());
                    }

                    return responses;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("POST")
                             .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                             .setBody(couchDb.mapper.writeValueAsString(map))
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper));
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
    public ListenableFuture<List<CouchDbPutResponse>> bulkRaw(final List<Map<String, Object>> docs) {
        try {
            Map<String, Object> map = new HashMap<>();

            map.put("docs", docs);

            Function<List<CouchDbPutResponse>, List<CouchDbPutResponse>> transformer = new Function<List<CouchDbPutResponse>, List<CouchDbPutResponse>>() {
                @Override
                public List<CouchDbPutResponse> apply(List<CouchDbPutResponse> responses) {
                    for (int i = 0; i < docs.size(); i++) {
                        docs.get(i).put("_id", responses.get(i).getDocId());
                        docs.get(i).put("_rev", responses.get(i).getRev());
                    }

                    return responses;
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setMethod("POST")
                             .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").build())
                             .setBody(couchDb.mapper.writeValueAsString(map))
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbPutResponse>>() {/* empty */}, transformer, couchDb.mapper));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //------------------ Admin API -------------------------

    public ListenableFuture<List<CouchDbDesignDocument>> getDesignDocs() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").async().asDocs();
    }

    /**
     * Create a new database.
     */
    public ListenableFuture<Boolean> createDb() {
        try {
            Function<CouchDbBooleanResponse, Boolean> transformer = new Function<CouchDbBooleanResponse, Boolean>() {
                @Override
                public Boolean apply(CouchDbBooleanResponse response) {
                    return response.isOk();
                }
            };

            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().build())
                             .setMethod("PUT")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, transformer, couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete an existing database.
     */
    public ListenableFuture<Boolean> deleteDb() {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().build())
                             .setMethod("DELETE")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns database information.
     */
    public ListenableFuture<CouchDbInfo> getInfo() {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().build())
                             .setMethod("GET")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbInfo>() {/* empty */}, new NoopFunction<CouchDbInfo>(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compaction compresses the database file by removing unused sections created during updates.
     * Old revisions of documents are also removed from the database though a small amount of meta
     * data is kept for use in conflict during replication.
     */
    public ListenableFuture<Boolean> compact() {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().addPathSegment("_compact").build())
                             .setMethod("POST")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts a compaction for all the views in the selected design document, requires admin privileges.
     */
    public ListenableFuture<Boolean> compactViews(String designName) {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().addPathSegment("_compact").addPathSegment(designName).build())
                             .setMethod("POST")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes view files that are not used by any design document, requires admin privileges.
     *
     * View indexes on disk are named after their MD5 hash of the view definition.
     * When you change a view, old indexes remain on disk. To clean up all outdated view indexes
     * (files named after the MD5 representation of views, that does not exist anymore) you can
     * trigger a view cleanup.
     */
    public ListenableFuture<Boolean> cleanupViews() {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().addPathSegment("_view_cleanup").build())
                             .setMethod("POST")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes sure all uncommited changes are written and synchronized to the disk.
     */
    public ListenableFuture<Boolean> ensureFullCommit() {
        try {
            return httpClient.prepareRequest(couchDb.prototype)
                             .setUrl(createUrlBuilder().addPathSegment("_ensure_full_commit").build())
                             .setMethod("POST")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
