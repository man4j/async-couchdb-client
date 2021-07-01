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

import com.equiron.acc.exception.http.CouchDbConflictException;
import com.equiron.acc.exception.http.CouchDbForbiddenException;
import com.equiron.acc.exception.http.CouchDbInternalServerErrorException;
import com.equiron.acc.json.CouchDbBooleanResponse;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAccessor;
import com.equiron.acc.json.CouchDbInfo;
import com.equiron.acc.json.CouchDbInstanceInfo;
import com.equiron.acc.profiler.CouchDbOperationStats;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.CouchDbBooleanResponseTransformer;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.rainerhahnekamp.sneakythrow.Sneaky;

public class CouchDbOperations {
    private CouchDb couchDb;

    private HttpClientProvider httpClient;
    
    private CouchDbOperationStats couchDbOperationStats;
    
    private Semaphore semaphore;

    public CouchDbOperations(CouchDb couchDb) {
        this.couchDb = couchDb;
        this.httpClient = couchDb.getHttpClientProvider();
        this.couchDbOperationStats = new CouchDbOperationStats(couchDb.getDbName());
        
        String maxParallelism = System.getProperty("COUCHDB_CLIENT_MAX_PARALLELISM");
        
        if (maxParallelism == null || maxParallelism.isBlank()) {
            maxParallelism = System.getenv("COUCHDB_CLIENT_MAX_PARALLELISM");
        }
        
        if (maxParallelism != null && !maxParallelism.isBlank()) {
            semaphore = new Semaphore(Integer.parseInt(maxParallelism));
        } else {
            semaphore = new Semaphore(128);
        }
    }
    
    public Semaphore getSemaphore() {
        return semaphore;
    }
    
    public CouchDbOperationStats getCouchDbOperationStats() {
        return couchDbOperationStats;
    }

    private UrlBuilder createUrlBuilder() {
        return new UrlBuilder(couchDb.getDbUrl());
    }
    
    //------------------ Fetch API -------------------------
    
    public <T extends CouchDbDocument> T get(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), false);
    }
    
    public <T extends CouchDbDocument> T get(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructType(CouchDbDocument.class), attachments);
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
            if (doc != null && doc instanceof CouchDbDocument) {
                new CouchDbDocumentAccessor((CouchDbDocument) doc).setCurrentDb(couchDb);
            }

            return doc;
        };
        
        OperationType operationType = attachments ? OperationType.GET_WITH_ATTACHMENT : OperationType.GET;
        
        OperationInfo operationInfo = new OperationInfo(operationType, 1, 0);
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.get(urlBuilder.build(), Map.of("Accept", attachments ? "application/json" : "*/*"));
            
            return new CouchDbResponseHandler<>(response, docType, transformer, couchDb.mapper, operationInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
    
    //------------------ Bulk API -------------------------

    public <T extends CouchDbDocument> List<T> saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) {
        T[] allDocs = ArrayUtils.insert(0, docs, doc);

        return saveOrUpdate(Arrays.asList(allDocs));
    }
    
    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs) {
        return saveOrUpdate(docs, false);
    }

    public <T extends CouchDbDocument> List<T> saveOrUpdate(List<T> docs, boolean ignoreConflicts) {
        CouchDbDocument[] allDocs = docs.toArray(new CouchDbDocument[] {});
        
        Function<List<CouchDbBulkResponse>, List<T>> transformer = responses -> {
            RuntimeException e = null;
            
            for (int i = 0; i < allDocs.length; i++) {
                CouchDbBulkResponse response = responses.get(i);
                
                if (!ignoreConflicts && response.isInConflict()) {
                    e = new CouchDbConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                } else if (response.isForbidden()) {
                    e = new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                } else if (!response.isInConflict() && response.getError() != null && !response.getError().isBlank()) {
                    e = new CouchDbInternalServerErrorException("Bulk error: " +  response.getError());
                }
                
                allDocs[i].setDocId(response.getDocId());
                allDocs[i].setRev(response.getRev());

                new CouchDbDocumentAccessor(allDocs[i]).setCurrentDb(couchDb)
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

            return new CouchDbResponseHandler<>(response, new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    public List<CouchDbBulkResponse> saveOrUpdate(@SuppressWarnings("unchecked") Map<String, Object>... docs) {
        Function<List<CouchDbBulkResponse>, List<CouchDbBulkResponse>> transformer = responses -> {
            RuntimeException e = null;
            
            for (int i = 0; i < docs.length; i++) {
                CouchDbBulkResponse response = responses.get(i);
                
                if (response.isInConflict()) {
                    e = new CouchDbConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                }
                
                if (response.isForbidden()) {
                    e = new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                }
                
                if (response.getError() != null && !response.getError().isBlank()) {
                    e = new CouchDbInternalServerErrorException("Bulk error: " +  response.getError());
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

            return new CouchDbResponseHandler<>(response, new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
    
    public List<CouchDbBulkResponse> delete(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        CouchDbDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return delete(Arrays.asList(allDocs));
    }
    
    public List<CouchDbBulkResponse> delete(List<CouchDbDocIdAndRev> docRevs) {
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
                    throw new CouchDbConflictException(response.getConflictReason() + " _docId: " + response.getDocId());
                }
                
                if (response.isForbidden()) {
                    throw new CouchDbForbiddenException("Forbidden: " +  response.getConflictReason());
                }
                
                if (response.getError() != null && !response.getError().isBlank()) {
                    throw new CouchDbInternalServerErrorException("Bulk error: " +  response.getError());
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

            return new CouchDbResponseHandler<>(response, new TypeReference<List<CouchDbBulkResponse>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
        
    public Map<String, Boolean> purge(CouchDbDocIdAndRev docRev, CouchDbDocIdAndRev... docRevs) {
        CouchDbDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return purge(Arrays.asList(allDocs));
    }

    public Map<String, Boolean> purge(List<CouchDbDocIdAndRev> docRevs) {
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
        
        String valueAsString = Sneaky.sneak(() -> couchDb.mapper.writeValueAsString(purgedMap));
        
        OperationInfo opInfo = new OperationInfo(OperationType.PURGE, docRevs.size(), valueAsString.length());
        
        int replicas = couchDb.getClusterInfo() == null ? 1 : couchDb.getClusterInfo().getN();
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_purge").addQueryParam("w", replicas + "").build(), valueAsString);

            return new CouchDbResponseHandler<>(response, new TypeReference<Map<String, Object>>() {/* empty */}, transformer, couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    //------------------ Attach API -------------------------

    public CouchDbBulkResponse attach(CouchDbDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addPathSegment(name);

        if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
            urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
        }
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.put(urlBuilder.build(), in, Map.of("Content-Type", contentType));
            
            return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbBulkResponse>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
        
    public String getAttachmentAsString(String docId, String name) {
        OperationInfo opInfo = new OperationInfo(OperationType.GET_ATTACHMENT, 0, 0);
        
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        HttpClientProviderResponse r;
        
        try {
            r = httpClient.get(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build());
        } finally {
            semaphore.release();
        }
        
        try {
            opInfo.setStatus(r.getStatus());
            
            if (r.getStatus() == 200) {
                opInfo.setSize(r.getBody().length());
                return r.getBody();
            }
           
            if (r.getStatus() == 404) {
                return null;
            }
           
            throw CouchDbResponseHandler.responseCode2Exception(new CouchDbHttpResponse(r.getStatus(), r.getStatus() + "", r.getBody(), r.getUri().toString()));
        } finally {
            couchDbOperationStats.addOperation(opInfo);
        }
    }
    
    public byte[] getAttachmentAsBytes(String docId, String name) {
        OperationInfo opInfo = new OperationInfo(OperationType.GET_ATTACHMENT, 0, 0);
        
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        HttpClientProviderResponse r;
        
        try {
            r = httpClient.getBytes(createUrlBuilder().addPathSegment(docId).addPathSegment(name).build());
        } finally {
            semaphore.release();
        }
        
        try {
            opInfo.setStatus(r.getStatus());
            
            if (r.getStatus() == 200) {
                opInfo.setSize(r.getBodyAsBytes().length);
                return r.getBodyAsBytes();
            }
           
            if (r.getStatus() == 404) {
                return null;
            }
           
            throw CouchDbResponseHandler.responseCode2Exception(new CouchDbHttpResponse(r.getStatus(), r.getStatus() + "", r.getBody(), r.getUri().toString()));
        } finally {
            couchDbOperationStats.addOperation(opInfo);
        }
    }

    public Boolean deleteAttachment(CouchDbDocIdAndRev docIdAndRev, String name) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId())
                                                  .addPathSegment(name)
                                                  .addQueryParam("rev", docIdAndRev.getRev());
        
        OperationInfo opInfo = new OperationInfo(OperationType.DELETE_ATTACHMENT, 0, 0);
        
        Sneaky.sneak(() -> { semaphore.acquire(); return true;} );
        
        try {
            HttpClientProviderResponse response = httpClient.delete(urlBuilder.build());
            
            return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper, opInfo, couchDbOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    //------------------ Admin API -------------------------

    public List<CouchDbDesignDocument> getDesignDocs() {
        return couchDb.getBuiltInView().<CouchDbDesignDocument>createDocQuery().startKey("_design/").endKey("_design0").asDocs().stream().filter(d -> d.getValidateDocUpdate() == null || d.getValidateDocUpdate().isBlank()).collect(Collectors.toList());
    }
    
    public List<String> getDatabases() {
        HttpClientProviderResponse response = httpClient.get(new UrlBuilder(couchDb.getServerUrl()).addPathSegment("_all_dbs").build());
        
        return new CouchDbResponseHandler<>(response, new TypeReference<List<String>>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean createDb() {
        HttpClientProviderResponse response = httpClient.put(createUrlBuilder().build(), "");

        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbBooleanResponse>() {/* empty */}, resp -> resp.isOk(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean deleteDb() {
        HttpClientProviderResponse response = httpClient.delete(createUrlBuilder().build());

        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public CouchDbInfo getInfo() {
        HttpClientProviderResponse response = httpClient.get(createUrlBuilder().build());

        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbInfo>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }
    
    public CouchDbInstanceInfo getInstanceInfo() {
        HttpClientProviderResponse response = httpClient.get(couchDb.getServerUrl());

        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbInstanceInfo>() {/* empty */}, Function.identity(), couchDb.mapper, null, couchDbOperationStats).transform();
    }

    public Boolean cleanupViews() {
        HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_view_cleanup").build(), "");
        
        return new CouchDbResponseHandler<>(response, new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), couchDb.mapper, null, couchDbOperationStats).transform();
    }
}