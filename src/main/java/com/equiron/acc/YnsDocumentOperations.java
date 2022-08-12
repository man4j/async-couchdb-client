package com.equiron.acc;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import com.equiron.acc.exception.YnsBulkException;
import com.equiron.acc.exception.YnsBulkRuntimeException;
import com.equiron.acc.json.YnsBooleanResponse;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAccessor;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.transformer.YnsBooleanResponseTransformer;
import com.equiron.acc.util.StreamResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.SneakyThrows;

public class YnsDocumentOperations extends YnsAbstractOperations implements YnsDocumentOperationsInterface {
    public YnsDocumentOperations(YnsDb ynsDb) {
        super(ynsDb);
    }
    
    //------------------ Fetch API -------------------------
    
    @Override
    public <T extends YnsDocument> T get(String docId, String...allDocs docIds) {
        return get(docId, TypeFactory.defaultInstance().constructType(YnsDocument.class), false);
    }
    
    @Override
    public <T extends YnsDocument> T get(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructType(YnsDocument.class), attachments);
    }
    
    @Override
    public Map<String, Object> getRaw(String docId) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), false);
    }
    
    @Override
    public Map<String, Object> getRaw(String docId, boolean attachments) {
        return get(docId, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class), attachments);
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
        YnsDocument[] allDocs = docs.toArray(new YnsDocument[] {});
        
        Function<List<YnsBulkResponse>, List<T>> transformer = responses -> {
            for (int i = 0; i < allDocs.length; i++) {
                YnsBulkResponse response = responses.get(i);
                
                allDocs[i].setDocId(response.getDocId());
                allDocs[i].setRev(response.getRev());

                new YnsDocumentAccessor(allDocs[i]).setCurrentDb(ynsDb)
                                                   .setInConflict(response.isInConflict())
                                                   .setForbidden(response.isForbidden())
                                                   .setBulkError(response.getError())
                                                   .setConflictReason(response.getConflictReason());
            }

            for (var r : responses) {
                if (!r.getError().isBlank()) {
                    throw new YnsBulkRuntimeException(responses);
                }
            }

            return docs;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", allDocs));
        
        OperationInfo opInfo = new OperationInfo(OperationType.INSERT_UPDATE, allDocs.length, valueAsString.length());
        
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

    @Override
    public List<YnsBulkResponse> saveOrUpdateRaw(Map<String, Object> doc, @SuppressWarnings("unchecked") Map<String, Object>... docs) throws YnsBulkException {
        Map<String, Object>[] allDocs = ArrayUtils.insert(0, docs, doc);

        return saveOrUpdateRaw(Arrays.asList(allDocs));
    }

    @Override
    @SneakyThrows
    public List<YnsBulkResponse> saveOrUpdateRaw(List<Map<String, Object>> docs) throws YnsBulkException {
        Map<String, Object>[] allDocs = docs.toArray(new Map[] {});
        
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            for (int i = 0; i < allDocs.length; i++) {
                YnsBulkResponse response = responses.get(i);

                allDocs[i].put("_id", response.getDocId());
                allDocs[i].put("_rev", response.getRev());
            }
            
            for (var r : responses) {
                if (!r.getError().isBlank()) {
                    throw new YnsBulkRuntimeException(responses);
                }
            }
            
            return responses;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", docs));
        
        OperationInfo opInfo = new OperationInfo(OperationType.INSERT_UPDATE, allDocs.length, valueAsString.length());
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").build(), valueAsString);

            try {
                return new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
            } catch (YnsBulkRuntimeException e) {
                throw new YnsBulkException(e.getResponses());
            }
        } finally {
            semaphore.release();
        }
    }    
    
    //------------------ Delete API -------------------------

    @Override
    public List<YnsBulkResponse> delete(YnsDocIdAndRev docRev, YnsDocIdAndRev... docRevs) throws YnsBulkException {
        YnsDocIdAndRev[] allDocs = ArrayUtils.insert(0, docRevs, docRev);
        
        return delete(Arrays.asList(allDocs));
    }
    
    @Override
    @SneakyThrows
    public List<YnsBulkResponse> delete(List<YnsDocIdAndRev> docRevs) throws YnsBulkException {
        List<YnsDocument> docsWithoutBody = docRevs.stream().map(dr -> {
            YnsDocument dummyDoc = new YnsDocument();
            
            dummyDoc.setDocId(dr.getDocId());
            dummyDoc.setRev(dr.getRev());
            dummyDoc.setDeleted(true);
            
            return dummyDoc;
        }).toList();
        
        Function<List<YnsBulkResponse>, List<YnsBulkResponse>> transformer = responses -> {
            for (var r : responses) {
                if (!r.getError().isBlank()) {
                    throw new YnsBulkRuntimeException(responses);
                }
            }

            return responses;
        };
        
        String valueAsString = ynsDb.mapper.writeValueAsString(Collections.singletonMap("docs", docsWithoutBody));
        
        OperationInfo opInfo = new OperationInfo(OperationType.DELETE, docRevs.size(), valueAsString.length());
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.post(createUrlBuilder().addPathSegment("_bulk_docs").build(), valueAsString);
            
            try {
                return new YnsResponseHandler<>(response, new TypeReference<List<YnsBulkResponse>>() {/* empty */}, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
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