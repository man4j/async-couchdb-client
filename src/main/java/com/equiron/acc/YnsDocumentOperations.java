package com.equiron.acc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import com.equiron.acc.exception.YnsBulkException;
import com.equiron.acc.exception.YnsBulkGetException;
import com.equiron.acc.exception.YnsBulkGetRuntimeException;
import com.equiron.acc.exception.YnsBulkRuntimeException;
import com.equiron.acc.json.YnsBooleanResponse;
import com.equiron.acc.json.YnsBulkGetErrorResult;
import com.equiron.acc.json.YnsBulkGetResponse;
import com.equiron.acc.json.YnsBulkGetResult;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAccessor;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.YnsBooleanResponseTransformer;
import com.equiron.acc.util.StreamResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.SneakyThrows;

public class YnsDocumentOperations implements YnsDocumentOperationsInterface {
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
    
    //------------------ Fetch API -------------------------
    
    @Override
    public <T extends YnsDocument> List<T> get(String docId, String... docIds) throws YnsBulkGetException {
        String[] allIds = ArrayUtils.insert(0, docIds, docId);
        
        return get(Arrays.asList(allIds));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends YnsDocument> List<T> get(List<String> docIds) throws YnsBulkGetException {
        return (List<T>) get(docIds, false);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends YnsDocument> List<T> get(List<String> docIds, boolean attachments) throws YnsBulkGetException {
        return (List<T>) get(docIds, attachments, new TypeReference<YnsBulkGetResponse<YnsDocument>>() {/* empty */}, false);
    }
    
    //------------------ Fetch RAW API -------------------------

    @Override
    public List<Map<String, Object>> getRaw(String docId, String... docIds) throws YnsBulkGetException {
        String[] allIds = ArrayUtils.insert(0, docIds, docId);
        
        return getRaw(Arrays.asList(allIds));
    }
    
    @Override
    public List<Map<String, Object>> getRaw(List<String> docIds) throws YnsBulkGetException {
        return getRaw(docIds, false);
    }
    
    @Override
    public List<Map<String, Object>> getRaw(List<String> docIds, boolean attachments) throws YnsBulkGetException {
        return get(docIds, attachments, new TypeReference<YnsBulkGetResponse<Map<String, Object>>>() {/* empty */}, true);
    }
    
    @SneakyThrows
    public <T> List<T> get(List<String> docIds, boolean attachments, TypeReference<YnsBulkGetResponse<T>> listOfDocs, boolean raw) {
        @SuppressWarnings({ "unchecked", "cast" })
        Function<YnsBulkGetResponse<T>, List<T>> transformer = responses -> {
            Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap = new HashMap<>();
            Map<String, YnsBulkGetErrorResult> errorDocumentsMap = new HashMap<>();

            List<T> docs = new ArrayList<>();
            
            for (int i = 0; i < docIds.size(); i++) {
                YnsBulkGetResult<T> result = responses.getResults().get(i);
                
                if (result.getDocs().size() > 1) {
                    T doc = result.getDocs().get(0).getOk();
                    
                    if (!raw) {
                        new YnsDocumentAccessor((YnsDocument) doc).setCurrentDb(ynsDb);

                        conflictingDocumentsMap.put(result.getDocId(), result.getDocs().stream().map(d -> ((YnsDocument)d.getOk()).getDocIdAndRev()).toList()); 
                    } else {
                        conflictingDocumentsMap.put(result.getDocId(), result.getDocs().stream().map(d -> new YnsDocIdAndRev((String)((Map<String, Object>)d.getOk()).get("_id"), (String)((Map<String, Object>)d.getOk()).get("_rev"))).toList()); 
                    }
                    
                } else {
                    if (result.getDocs().get(0).getOk() != null) {
                        T doc = (T) result.getDocs().get(0).getOk();

                        if (!raw) {
                            new YnsDocumentAccessor((YnsDocument) doc).setCurrentDb(ynsDb);
                        }
                        
                        docs.add(doc);
                    } else {//error
                        YnsBulkGetErrorResult errorResult = result.getDocs().get(0).getError();
                        
                        if (!errorResult.getError().equals("not_found")) {
                            errorDocumentsMap.put(errorResult.getDocId(), errorResult);
                        }
                    }
                }
            }

            if (!conflictingDocumentsMap.isEmpty() || !errorDocumentsMap.isEmpty()) {
                throw new YnsBulkGetRuntimeException(conflictingDocumentsMap, errorDocumentsMap);
            }

            return docs;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", docIds));
        
        OperationType operationType = attachments ? OperationType.GET_WITH_ATTACHMENT : OperationType.GET;
        
        OperationInfo opInfo = new OperationInfo(operationType, docIds.size(), 0);
        
        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment("_bulk_get");
        
        if (attachments) {
            urlBuilder.addQueryParam("attachments", "true");
        }

        semaphore.acquire();
        
        try {
            
            HttpClientProviderResponse response = httpClient.post(urlBuilder.build(), valueAsString); 

            try {
                return new YnsResponseHandler<>(response, listOfDocs, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
            } catch (YnsBulkGetRuntimeException e) {
                throw new YnsBulkGetException(e.getConflictingDocumentsMap(), e.getErrorDocumentsMap());
            }
        } finally {
            semaphore.release();
        }
    }

    //------------------ Bulk save or update API -------------------------

    @Override
    public <T extends YnsDocument> void saveOrUpdate(T doc, @SuppressWarnings("unchecked") T... docs) throws YnsBulkRuntimeException {
        T[] allDocs = ArrayUtils.insert(0, docs, doc);

        saveOrUpdate(Arrays.asList(allDocs));
    }
    
    @Override
    @SneakyThrows
    public <T extends YnsDocument> void saveOrUpdate(List<T> docs) throws YnsBulkRuntimeException {
        _saveOrUpdate(docs, OperationType.INSERT_UPDATE);
    }

    @Override
    public void saveOrUpdateRaw(Map<String, Object> doc, @SuppressWarnings("unchecked") Map<String, Object>... docs) throws YnsBulkException {
        Map<String, Object>[] allDocs = ArrayUtils.insert(0, docs, doc);

        saveOrUpdateRaw(Arrays.asList(allDocs));
    }
    
    @Override
    @SneakyThrows
    public void saveOrUpdateRaw(List<Map<String, Object>> docs) throws YnsBulkException {
        _saveOrUpdate(docs, OperationType.INSERT_UPDATE);
    }

    //------------------ Delete API -------------------------

    @Override
    public void delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) throws YnsBulkException {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        delete(Arrays.asList(allDocs));
    }
    
    @Override
    @SneakyThrows
    public void delete(List<YnsDocIdAndRev> docRevs) throws YnsBulkException {
        List<YnsDocument> docsWithoutBody = docRevs.stream().map(dr -> {
            YnsDocument dummyDoc = new YnsDocument();
            
            dummyDoc.setDocId(dr.getDocId());
            dummyDoc.setRev(dr.getRev());
            dummyDoc.setDeleted(true);
            
            return dummyDoc;
        }).toList();
        
        _saveOrUpdate(docsWithoutBody, OperationType.DELETE);
    }
    
    //-------------------------------------------------------
    
    @SneakyThrows
    private <T> void _saveOrUpdate(List<T> docs, OperationType opType) {
        Map<String, Object>[] allDocs = docs.toArray(new Map[] {});
        
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            for (int i = 0; i < allDocs.length; i++) {
                YnsBulkResponse response = responses.get(i);
                
                if (docs.get(0) instanceof YnsDocument) {
                    ((YnsDocument)allDocs[i]).setDocId(response.getDocId());
                    ((YnsDocument)allDocs[i]).setRev(response.getRev());
                    
                    new YnsDocumentAccessor((YnsDocument)allDocs[i]).setCurrentDb(ynsDb);
                } else {
                    allDocs[i].put("_id", response.getDocId());
                    allDocs[i].put("_rev", response.getRev());
                }
            }
            
            for (var r : responses) {
                if (!r.getError().isBlank()) {
                    throw new YnsBulkRuntimeException(responses);
                }
            }
            
            return responses;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", docs));
        
        OperationInfo opInfo = new OperationInfo(opType, allDocs.length, valueAsString.length());
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").build(), valueAsString);

            try {
                new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
            } catch (YnsBulkRuntimeException e) {
                throw new YnsBulkException(e.getResponses());
            }
        } finally {
            semaphore.release();
        }
    }    
    
    //------------------ Attach API -------------------------

    @Override
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
        
    @Override
    public StreamResponse getAttachmentAsStream(String docId, String name) {
        return getAttachmentAsStream(docId, name, null);
    }
    
    @SuppressWarnings("resource")
    @Override
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

    @Override
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