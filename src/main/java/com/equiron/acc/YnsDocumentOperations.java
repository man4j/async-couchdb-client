package com.equiron.acc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import com.equiron.acc.exception.YnsBulkDocumentException;
import com.equiron.acc.exception.YnsGetDocumentException;
import com.equiron.acc.json.YnsBooleanResponse;
import com.equiron.acc.json.YnsBulkGetErrorResult;
import com.equiron.acc.json.YnsBulkGetResponse;
import com.equiron.acc.json.YnsBulkGetResult;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocIdRevRequestItem;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.YnsBooleanResponseTransformer;
import com.equiron.acc.util.StreamResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.SneakyThrows;

public class YnsDocumentOperations {
    private YnsDb ynsDb;

    private HttpClientProvider httpClient;
    
    private YnsOperationStats ynsOperationStats;
    
    private Semaphore semaphore;
    
    public YnsDocumentOperations(YnsDb ynsDb) {
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

    protected UrlBuilder createUrlBuilder() {
        return new UrlBuilder(ynsDb.getDbUrl());
    }
    
    public YnsDb getYnsDb() {
        return ynsDb;
    }
    
    @SneakyThrows
    public <T> List<T> get(List<YnsDocIdAndRev> docIds, JavaType responseType, boolean raw) {
        @SuppressWarnings({ "unchecked", "cast" })
        Function<YnsBulkGetResponse<T>, List<T>> transformer = responses -> {
            Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap = new HashMap<>();
            Map<String, YnsBulkGetErrorResult> errorDocumentsMap = new HashMap<>();

            List<T> docs = new ArrayList<>();
            
            for (int i = 0; i < docIds.size(); i++) {
                YnsBulkGetResult<T> result = responses.getResults().get(i);
                
                if (result.getDocs().size() > 1) {
                    if (!raw) {
                        conflictingDocumentsMap.put(result.getDocId(), result.getDocs().stream().map(d -> ((YnsDocument)d.getOk()).getDocIdAndRev()).toList()); 
                    } else {
                        conflictingDocumentsMap.put(result.getDocId(), result.getDocs().stream().map(d -> new YnsDocIdAndRev((String)((Map<String, Object>)d.getOk()).get("_id"), (String)((Map<String, Object>)d.getOk()).get("_rev"))).toList()); 
                    }
                    
                } else {
                    if (result.getDocs().get(0).getOk() != null) {
                        T doc = (T) result.getDocs().get(0).getOk();
                        
                        Boolean deleted;
                        
                        if (raw) {
                            deleted = (Boolean) ((Map<String, Object>) doc).get("_deleted");
                        } else {
                            deleted = ((YnsDocument) doc).isDeleted();
                        }
                        
                        if (deleted == null || !deleted) {
                            docs.add(doc);
                        }
                    } else {//error
                        YnsBulkGetErrorResult errorResult = result.getDocs().get(0).getError();
                        
                        if (!errorResult.getError().equals("not_found")) {
                            errorDocumentsMap.put(errorResult.getDocId(), errorResult);
                        }
                    }
                }
            }

            if (!conflictingDocumentsMap.isEmpty() || !errorDocumentsMap.isEmpty()) {
                throw new YnsGetDocumentException(conflictingDocumentsMap, errorDocumentsMap);
            }

            return docs;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", docIds.stream().map(d -> new YnsDocIdRevRequestItem(d.getDocId(), d.getRev())).toList()));
        
        OperationInfo opInfo = new OperationInfo(OperationType.GET, docIds.size(), 0);
        
        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment("_bulk_get");
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.post(urlBuilder.build(), valueAsString); 

            return new YnsResponseHandler<>(response, responseType, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }

    //-------------------------------------------------------
    
    @SneakyThrows
    public <T> void saveOrUpdate(List<T> docs, OperationType opType, boolean raw) {
        @SuppressWarnings("unchecked")
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            for (int i = 0; i < docs.size(); i++) {
                YnsBulkResponse response = responses.get(i);
                
                if (!raw) {
                    ((YnsDocument)docs.get(i)).setDocId(response.getDocId());
                    ((YnsDocument)docs.get(i)).setRev(response.getRev());
                } else {
                    ((Map<String, Object>)docs.get(i)).put("_id", response.getDocId());
                    ((Map<String, Object>)docs.get(i)).put("_rev", response.getRev());
                }
            }
            
            for (var r : responses) {
                if (!r.getError().isBlank()) {
                    throw new YnsBulkDocumentException(responses);
                }
            }
            
            return responses;
        };
        
        ArrayNode arr = ynsDb.mapper.createArrayNode();
        
        docs.forEach(d -> arr.addPOJO(d));
        
        String valueAsString = ynsDb.mapper.writeValueAsString(ynsDb.mapper.createObjectNode().set("docs", arr));
        
        OperationInfo opInfo = new OperationInfo(opType, docs.size(), valueAsString.length());
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").build(), valueAsString);

            new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }    
    
    //------------------ Attach API -------------------------

    @SneakyThrows
    public YnsBulkResponse attach(YnsDocIdAndRev docIdAndRev, InputStream in, String name, String contentType) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId()).addPathSegment(name);

        if (docIdAndRev.getRev() != null && !docIdAndRev.getRev().isEmpty()) {
            urlBuilder.addQueryParam("rev", docIdAndRev.getRev());
        }
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.put(urlBuilder.build(), in, Map.of("Content-Type", contentType));
            
            return new YnsResponseHandler<>(response, new TypeReference<YnsBulkResponse>() {/* empty */}, Function.identity(), ynsDb.mapper, null, ynsOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
        
    @SuppressWarnings("resource")
    @SneakyThrows
    public StreamResponse getAttachmentAsStream(String docId, String name, Map<String, String> headers) {
        OperationInfo opInfo = new OperationInfo(OperationType.GET_ATTACHMENT, 0, 0);
        
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        
        semaphore.acquire();
        
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
            ynsOperationStats.addOperation(opInfo);
        }
    }

    @SneakyThrows
    public Boolean deleteAttachment(YnsDocIdAndRev docIdAndRev, String name) {
        if (docIdAndRev.getDocId() == null || docIdAndRev.getDocId().trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");
        if (docIdAndRev.getRev() == null || docIdAndRev.getRev().trim().isEmpty()) throw new IllegalStateException("The document revision cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docIdAndRev.getDocId())
                                                  .addPathSegment(name)
                                                  .addQueryParam("rev", docIdAndRev.getRev());
        
        OperationInfo opInfo = new OperationInfo(OperationType.DELETE_ATTACHMENT, 0, 0);
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.delete(urlBuilder.build());
            
            return new YnsResponseHandler<>(response, new TypeReference<YnsBooleanResponse>() {/* empty */}, new YnsBooleanResponseTransformer(), ynsDb.mapper, opInfo, ynsOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
}