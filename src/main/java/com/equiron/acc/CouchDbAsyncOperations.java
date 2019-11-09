package com.equiron.acc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import com.equiron.acc.exception.http.CouchDbConflictException;
import com.equiron.acc.exception.http.CouchDbForbiddenException;
import com.equiron.acc.json.CouchDbBooleanResponse;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAccessor;
import com.equiron.acc.json.CouchDbInfo;
import com.equiron.acc.transformer.CouchDbBooleanResponseTransformer;
import com.equiron.acc.util.FutureUtils;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class CouchDbAsyncOperations {
    private CouchDb couchDb;

    private AsyncHttpClient httpClient;

    public CouchDbAsyncOperations(CouchDb couchDb) {
        this.couchDb = couchDb;
        this.httpClient = couchDb.getHttpClient();
    }

    private UrlBuilder createUrlBuilder() {
        return new UrlBuilder(couchDb.getDbUrl());
    }
    
    //------------------ Fetch API -------------------------
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> get(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), false);
    }
    
    /**
     * Returns the latest revision of the document.
     */
    public <T extends CouchDbDocument> CompletableFuture<T> get(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), attachments);
    }

    /**
     * Returns the latest revision of the document.
     */
    public CompletableFuture<Map<String, Object>> getRaw(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), false);
    }
    
    /**
     * Returns the latest revision of the document.
     */
    public CompletableFuture<Map<String, Object>> getRaw(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), attachments);
    }
    
    /**
     * Returns the latest revision of the document.
     */
    private <T> CompletableFuture<T> get(String docId, JavaType docType, boolean attachments) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docId);
        
        if (attachments) {
            urlBuilder.addQueryParam("attachments", "true");
        }

        Function<T, T> transformer = doc -> {
            if (doc != null && doc instanceof CouchDbDocument) {
                new CouchDbDocumentAccessor((CouchDbDocument) doc).setCurrentDb(couchDb);
            }

            return doc;
        };

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setHeader("Accept", attachments ? "application/json" : "*/*")
                                                   .setMethod("GET")
                                                   .setUrl(urlBuilder.build())
                                                   .execute(new CouchDbAsyncHandler<>(docType, transformer, couchDb.mapper)));
    }
    
    //------------------ Bulk API -------------------------

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> CompletableFuture<List<T>> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        T[] allDocs = ArrayUtils.insert(0, docs, doc);

        return saveOrUpdate(Arrays.asList(allDocs));
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public <T extends CouchDbDocument> CompletableFuture<List<T>> saveOrUpdate(List<T> docs) {
        try {
            CouchDbDocument[] allDocs = docs.toArray(new CouchDbDocument[] {});
            
            Function<List<CouchDbBulkResponse>, List<T>> transformer = responses -> {
                RuntimeException e = null;
                
                for (int i = 0; i < allDocs.length; i++) {
                    CouchDbBulkResponse response = responses.get(i);
                    
                    if (response.isInConflict()) {
                        e = new CouchDbConflictException(response.getConflictReason() + ": one of documents in conflict state. Please use doc.isInConflict() to resolve this situation.");
                    }
                    
                    if (response.isForbidden()) {
                        e = new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                    }
                    
                    allDocs[i].setDocId(response.getDocId());
                    allDocs[i].setRev(response.getRev());

                    new CouchDbDocumentAccessor(allDocs[i]).setCurrentDb(couchDb)
                                                           .setInConflict(response.isInConflict())
                                                           .setForbidden(response.isForbidden())
                                                           .setConflictReason(response.getConflictReason());
                }
                
                if (e != null) throw e;

                return docs;
            };

            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", couchDb.getClusterInfo().getN() + "").build())
                                                       .setBody(couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", allDocs)))
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Insert or update multiple documents in to the database in a single request.
     */
    public CompletableFuture<List<CouchDbBulkResponse>> saveOrUpdate(@SuppressWarnings("unchecked") Map<String, Object>... docs) {
        try {
            Function<List<CouchDbBulkResponse>, List<CouchDbBulkResponse>> transformer = responses -> {
                RuntimeException e = null;
                
                for (int i = 0; i < docs.length; i++) {
                    CouchDbBulkResponse response = responses.get(i);
                    
                    if (response.isInConflict()) {
                        e = new CouchDbConflictException(response.getConflictReason() + ": one of documents in conflict state. Please use doc.isInConflict() to resolve this situation.");
                    }
                    
                    if (response.isForbidden()) {
                        e = new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                    }
                    
                    docs[i].put("_id", response.getDocId());
                    docs[i].put("_rev", response.getRev());
                    docs[i].put("conflictReason", response.getConflictReason());
                    docs[i].put("reason", response.getConflictReason());
                    docs[i].put("error", response.getError());
                }
                
                if (e != null) throw e;

                return responses;
            };

            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", couchDb.getClusterInfo().getN() + "").build())
                                                       .setBody(couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", docs)))
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public CompletableFuture<List<CouchDbBulkResponse>> delete(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        CouchDbDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return delete(Arrays.asList(allDocs));
    }
    
    /**
     * Delete multiple documents from the database in a single request.
     */
    public CompletableFuture<List<CouchDbBulkResponse>> delete(List<CouchDbDocIdAndRev> docRevs) {
        try {
            List<CouchDbDocument> docsWithoutBody = docRevs.stream().map(dr -> {
                CouchDbDocument dummyDoc = new CouchDbDocument();
                
                dummyDoc.setDocId(dr.getDocId());
                dummyDoc.setRev(dr.getRev());
                dummyDoc.setDeleted();
                
                return dummyDoc;
            }).collect(Collectors.toList());
            
            Function<List<CouchDbBulkResponse>, List<CouchDbBulkResponse>> transformer = responses -> {
                for (int i = 0; i < docRevs.size(); i++) {
                    CouchDbBulkResponse response = responses.get(i);
                    
                    if (response.isInConflict()) {
                        throw new CouchDbConflictException(response.getConflictReason() + ": one of documents in conflict state. Please use doc.isInConflict() to resolve this situation.");
                    }
                    
                    if (response.isForbidden()) {
                        throw new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                    }
                }

                return responses;
            };
            
            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", couchDb.getClusterInfo().getN() + "").build())
                                                       .setBody(couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", docsWithoutBody)))
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
        
    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public CompletableFuture<Map<String, Boolean>> purge(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        CouchDbDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return purge(Arrays.asList(allDocs));
    }

    /**
     * As a result of this purge operation, a document will be completely removed from the database’s 
     * document b+tree, and sequence b+tree. It will not be available through _all_docs or _changes endpoints, 
     * as though this document never existed. Also as a result of purge operation, the database’s purge_seq 
     * and update_seq will be increased.
     */
    public CompletableFuture<Map<String, Boolean>> purge(List<CouchDbDocIdAndRev> docRevs) {
        try {
            Map<String, List<String>> purgedMap = new LinkedHashMap<>();
            
            for (CouchDbDocIdAndRev docRev : docRevs) {
                purgedMap.put(docRev.getDocId(), Collections.singletonList(docRev.getRev()));
            }
            
            Function<Map<String, Object>, Map<String, Boolean>> transformer = response -> {
                Map<String, Boolean> result = new HashMap<>();
                
                @SuppressWarnings("unchecked")
                Map<String, List<String>> revsMap = (Map<String, List<String>>) response.get("purged");
                
                revsMap.forEach((String docId, List<String> revs) -> {
                    result.put(docId, revs.isEmpty() ? false : true);
                });
                
                return result;
            };
        
            return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                       .setMethod("POST")
                                                       .setUrl(createUrlBuilder().addPathSegment("_purge").addQueryParam("w", couchDb.getClusterInfo().getN() + "").build())
                                                       .setBody(couchDb.mapper.writeValueAsString(purgedMap))
                                                       .execute(new CouchDbAsyncHandler<>(new TypeReference<Map<String, Object>>() {/* empty */}, transformer, couchDb.mapper)));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    //------------------ Attach API -------------------------

    /**
     * Attach content to the document.
     */
    public CompletableFuture<CouchDbBulkResponse> attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
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
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBulkResponse>() {/* empty */}, Function.identity(), couchDb.mapper)));
    }

    /**
     * Attach content to non-existing document.
     */
    public CompletableFuture<CouchDbBulkResponse> attach(String docId, InputStream in, String name, String contentType) {
        return attach(new CouchDbDocIdAndRev(docId, null), in, name, contentType);
    }

    /**
     * Gets an attachment of the document as Response.
     */
    public CompletableFuture<Response> getAttachment(String docId, String name) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setMethod("GET")
                                                   .setUrl(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build())
                                                   .execute());
    }
    
    /**
     * Gets an attachment of the document as String.
     */
    public CompletableFuture<String> getAttachmentAsString(String docId, String name) {
        return getAttachment(docId, name).thenApply(r -> {
           if (r.getStatusCode() == 200) {
               return r.getResponseBody();
           }
           
           if (r.getStatusCode() == 404) {
               return null;
           }
           
           CouchDbHttpResponse response = new CouchDbHttpResponse(r.getStatusCode(), r.getStatusText(), r.getResponseBody(StandardCharsets.UTF_8), r.getUri().toString(), r.getHeaders());
           
           throw CouchDbAsyncHandler.responseCode2Exception(response);
        });
    }
    
    /**
     * Gets an attachment of the document as bytes.
     */
    public CompletableFuture<byte[]> getAttachmentAsBytes(String docId, String name) {
        return getAttachment(docId, name).thenApply(r -> {
           if (r.getStatusCode() == 200) {
               return r.getResponseBodyAsBytes();
           }
           
           if (r.getStatusCode() == 404) {
               return null;
           }
           
           CouchDbHttpResponse response = new CouchDbHttpResponse(r.getStatusCode(), r.getStatusText(), r.getResponseBody(StandardCharsets.UTF_8), r.getUri().toString(), r.getHeaders());
           
           throw CouchDbAsyncHandler.responseCode2Exception(response);
        });
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

    //------------------ Admin API -------------------------

    public CompletableFuture<List<CouchDbDesignDocument>> getDesignDocs() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").async().asDocs();
    }
    
    /**
     * Returns a list of databases on this server.
     */
    public CompletableFuture<List<String>> getDatabases() {
        return FutureUtils.toCompletable(httpClient.prepareRequest(couchDb.prototype)
                                                   .setUrl(new UrlBuilder(couchDb.getServerUrl()).addPathSegment("_all_dbs").build())
                                                   .setMethod("GET")
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<List<String>>() {/* empty */}, Function.identity(), couchDb.mapper)));
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
                                                   .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbInfo>() {/* empty */}, Function.identity(), couchDb.mapper)));
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
}