package com.equiron.acc;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.equiron.acc.exception.http.YnsConflictException;
import com.equiron.acc.exception.http.YnsForbiddenException;
import com.equiron.acc.exception.http.YnsInternalServerErrorException;
import com.equiron.acc.json.YnsBooleanResponse;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAccessor;
import com.equiron.acc.json.YnsDbInfo;
import com.equiron.acc.json.YnsInstanceInfo;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.YnsBooleanResponseTransformer;
import com.equiron.acc.util.StreamResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.rainerhahnekamp.sneakythrow.Sneaky;

public class YnsOperations {
    private YnsDb ynsDb;

    private HttpClientProvider httpClient;
    
    private YnsOperationStats ynsOperationStats;
    
    private Semaphore semaphore;
    
    public YnsOperations(YnsDb ynsDb) {
        this.ynsDb = ynsDb;
        this.httpClient = ynsDb.getHttpClientProvider();
        this.ynsOperationStats = new YnsOperationStats(ynsDb.getDbName());
        semaphore = new Semaphore(ynsDb.getConfig().getClientMaxParallelism());
    }
    
    public Semaphore getSemaphore() {
        return semaphore;
    }
    
    public YnsOperationStats getYnsOperationStats() {
        return ynsOperationStats;
    }

    private UrlBuilder createUrlBuilder() {
        return new UrlBuilder(ynsDb.getDbUrl());
    }
    
    //------------------ Fetch API -------------------------
    
    public <T extends YnsDocument> T get(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructType(YnsDocument.class), false);
    }
    
    public <T extends YnsDocument> T get(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructType(YnsDocument.class), attachments);
    }

    public Map<String, Object> getRaw(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), false);
    }
    
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), attachments);
    }
    
    private <T> T get(String docId, JavaType docType, boolean attachments) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docId);
        
        if (attachments) {
            urlBuilder.addQueryParam("attachments", "true");
        }

        Function<T, T> transformer = doc -> {
            if (doc != null && doc instanceof YnsDocument) {
                new YnsDocumentAccessor((YnsDocument) doc).setCurrentDb(couchDb);
            }

            return doc;
        };
        
        OperationType operationType = attachments ? OperationType.GET_WITH_ATTACHMENT : OperationType.GET;
        
        OperationInfo operationInfo = new OperationInfo(operationType, 1, 0);
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.get(urlBuilder.build(), Map.of("Accept", attachments ? "application/json" : "*/*"));
            
            return new YnsResponseHandler<>(response, docType, transformer, couchDb.mapper, operationInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
    
    //------------------ Bulk API -------------------------

    public <T extends YnsDocument> List<T> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        T[] allDocs = ArrayUtils.insert(0, docs, doc);

        return saveOrUpdate(Arrays.asList(allDocs));
    }
    
    public <T extends YnsDocument> List<T> saveOrUpdate(List<T> docs) {
        return saveOrUpdate(docs, false);
    }

    public <T extends YnsDocument> List<T> saveOrUpdate(List<T> docs, boolean ignoreConflicts) {
        YnsDocument[] allDocs = docs.toArray(new YnsDocument[] {});
        
        Function<List<YnsBulkResponse>, List<T>> transformer = responses -> {
            RuntimeException e = null;
            
            for (int i = 0; i < allDocs.length; i++) {
                YnsBulkResponse response = responses.get(i);
                
                if (!ignoreConflicts && response.isInConflict()) {
                    e = new YnsConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                } else if (response.isForbidden()) {
                    e = new YnsForbiddenException("Forbidden: " +  response.getConflictReason());
                } else if (!response.isInConflict() && response.getError() != null && !response.getError().isBlank()) {
                    e = new YnsInternalServerErrorException("Bulk error: " +  response.getError());
                }
                
                allDocs[i].setDocId(response.getDocId());
                allDocs[i].setRev(response.getRev());

                new YnsDocumentAccessor(allDocs[i]).setCurrentDb(couchDb)
                                                       .setInConflict(response.isInConflict())
                                                       .setForbidden(response.isForbidden())
                                                       .setBulkError(response.getError())
                                                       .setConflictReason(response.getConflictReason());
            }
            
            if (e != null) throw e;

            return docs;
        };
        
        String valueAsString = Sneaky.sneak(() -> couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", allDocs)));
        
        OperationInfo opInfo = new OperationInfo(OperationType.INSERT_UPDATE, allDocs.length, valueAsString.length());
        
        int replicas = couchDb.getClusterInfo() == null ? 1 : couchDb.getClusterInfo().getN();
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", replicas + "").build(), valueAsString); 

            return new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    public List<YnsBulkResponse> saveOrUpdate(@SuppressWarnings("unchecked") Map<String, Object>... docs) {
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            RuntimeException e = null;
            
            for (int i = 0; i < docs.length; i++) {
                YnsBulkResponse response = responses.get(i);
                
                if (response.isInConflict()) {
                    e = new YnsConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                }
                
                if (response.isForbidden()) {
                    e = new YnsForbiddenException("Forbidden: " +  response.getConflictReason());
                }
                
                if (response.getError() != null && !response.getError().isBlank()) {
                    e = new YnsInternalServerErrorException("Bulk error: " +  response.getError());
                }
                
                docs[i].put("_id", response.getDocId());
                docs[i].put("_rev", response.getRev());
            }
            
            if (e != null) throw e;

            return responses;
        };
        
        String valueAsString = Sneaky.sneak(() -> couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", docs)));
        
        OperationInfo opInfo = new OperationInfo(OperationType.INSERT_UPDATE, docs.length, valueAsString.length());
        
        int replicas = couchDb.getClusterInfo() == null ? 1 : couchDb.getClusterInfo().getN();
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", replicas + "").build(), valueAsString);

            return new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
    
    public List<YnsBulkResponse> delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return delete(Arrays.asList(allDocs));
    }
    
    public List<YnsBulkResponse> delete(List<YnsDocIdAndRev> docRevs) {
        List<YnsDocument> docsWithoutBody = docRevs.stream().map(dr -> {
            YnsDocument dummyDoc = new YnsDocument();
            
            dummyDoc.setDocId(dr.getDocId());
            dummyDoc.setRev(dr.getRev());
            dummyDoc.setDeleted();
            
            return dummyDoc;
        }).collect(Collectors.toList());
        
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            for (int i = 0; i < docRevs.size(); i++) {
                YnsBulkResponse response = responses.get(i);
                
                if (response.isInConflict()) {
                    throw new YnsConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                }
                
                if (response.isForbidden()) {
                    throw new YnsForbiddenException("Forbidden: " +  response.getConflictReason());
                }
                
                if (response.getError() != null && !response.getError().isBlank()) {
                    throw new YnsInternalServerErrorException("Bulk error: " +  response.getError());
                }
            }

            return responses;
        };
        
        String valueAsString = Sneaky.sneak(() -> couchDb.mapper.writeValueAsString(Collections.singletonMap("docs", docsWithoutBody)));
        
        OperationInfo opInfo = new OperationInfo(OperationType.DELETE, docRevs.size(), valueAsString.length());
        
        int replicas = couchDb.getClusterInfo() == null ? 1 : couchDb.getClusterInfo().getN();
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").addQueryParam("w", replicas + "").build(), valueAsString);

            return new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
        
    public Map<String, Boolean> purge(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return purge(Arrays.asList(allDocs));
    }

    public Map<String, Boolean> purge(List<YnsDocIdAndRev> docRevs) {
        Map<String, List<String>> purgedMap = new LinkedHashMap<>();
        
        for (YnsDocIdAndRev docRev : docRevs) {
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
        
        String valueAsString = Sneaky.sneak(() -> couchDb.mapper.writeValueAsString(purgedMap));
        
        OperationInfo opInfo = new OperationInfo(OperationType.PURGE, docRevs.size(), valueAsString.length());
        
        int replicas = couchDb.getClusterInfo() == null ? 1 : couchDb.getClusterInfo().getN();
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_purge").addQueryParam("w", replicas + "").build(), valueAsString);

            return new YnsResponseHandler<>(response, new TypeReference<Map<String, Object>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    //------------------ Attach API -------------------------

    public YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addPathSegment(name);

        if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
            urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
        }
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.put(urlBuilder.build(), in, Map.of("Content-Type", contentType));
            
            return new YnsResponseHandler<>(response, new TypeReference<YnsBulkResponse>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
        
    public StreamResponse getAttachmentAsStream(String docId, String name) {
        return getAttachmentAsStream(docId, name, null);
    }
    
    public StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers) {
        OperationInfo opInfo = new OperationInfo(OperationType.GET_ATTACHMENT, 0, 0);
        
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        HttpClientProviderResponse r;
        
        try {
            r = httpClient.getStream(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build(), "GET", "", headers);
        } finally {
            semaphore.release();
        }
        
        try {
            opInfo.setStatus(r.getStatus());
            
            if (r.getStatus() == 200 || r.getStatus() == 304) {
                if (r.getHeaders().get("content-length") != null) {
                    opInfo.setSize(Long.parseLong(r.getHeaders().get("content-length")));
                }
                
                return new StreamResponse(r.getIn(), r.getHeaders(), r.getStatus());
            }
            
            if (r.getStatus() == 404) {
                return null;
            }
           
            throw YnsResponseHandler.responseCode2Exception(new YnsHttpResponse(r.getStatus(), r.getStatus() + "", r.getBody(), r.getUri().toString()));
        } finally {
            couchDbOperationStats.addOperation(opInfo);
        }
    }

    public Boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId())
                                                  .addPathSegment(name)
                                                  .addQueryParam("rev", docIdAndRev.getRev());
        
        OperationInfo opInfo = new OperationInfo(OperationType.DELETE_ATTACHMENT, 0, 0);
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.delete(urlBuilder.build());
            
            return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").asDocs().stream().filter(d -> d.getValidateDocUpdate() == null || d.getValidateDocUpdate().isBlank()).collect(Collectors.toList());
    }
    
    public List<CouchDbDesignDocument> getDesignDocsWithValidators() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").asDocs().stream().collect(Collectors.toList());
    }
    
    public List<String> getDatabases() {
        HttpClientProviderResponse response = httpClient.get(new UrlBuilder(couchDb.getServerUrl()).addPathSegment("_all_dbs").build());
        
        return new YnsResponseHandler<>(response, new TypeReference<List<String>>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean createDb() {
        HttpClientProviderResponse response = httpClient.put(createUrlBuilder().build(), "");

        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, resp -> resp.isOk(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean deleteDb() {
        HttpClientProviderResponse response = httpClient.delete(createUrlBuilder().build());

        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public YnsDbInfo getInfo() {
        HttpClientProviderResponse response = httpClient.get(createUrlBuilder().build());

        return new YnsResponseHandler<>(response, new TypeReference<YnsDbInfo>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }
    
    public YnsInstanceInfo getInstanceInfo() {
        HttpClientProviderResponse response = httpClient.get(couchDb.getServerUrl());

        return new YnsResponseHandler<>(response, new TypeReference<YnsInstanceInfo>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean cleanupViews() {
        HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_view_cleanup").build(), "");
        
        return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), couchDb.mapper, null, couchDbOperationStats).transform();
    }
}