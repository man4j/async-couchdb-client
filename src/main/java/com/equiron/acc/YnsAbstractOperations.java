package com.equiron.acc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import com.equiron.acc.exception.YnsBulkGetException;
import com.equiron.acc.exception.YnsBulkGetRuntimeException;
import com.equiron.acc.json.YnsBulkGetErrorResultItem;
import com.equiron.acc.json.YnsBulkGetResponse;
import com.equiron.acc.json.YnsBulkGetResult;
import com.equiron.acc.json.YnsBulkGetResultItem;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAccessor;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.SneakyThrows;

public class YnsAbstractOperations {
    YnsDb ynsDb;

    HttpClientProvider httpClient;
    
    YnsOperationStats ynsOperationStats;
    
    Semaphore semaphore;
    
    public YnsAbstractOperations(YnsDb ynsDb) {
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
    
    @SneakyThrows
    protected <T extends YnsDocument> List<T> get(List<String> docIds, boolean attachments) throws YnsBulkGetException {
        @SuppressWarnings({ "unchecked", "cast" })
        Function<YnsBulkGetResponse, List<T>> transformer = responses -> {
            Map<String, List<T>> conflictingDocumentsMap = new HashMap<>();
            Map<String, YnsBulkGetErrorResultItem> errorDocumentsMap = new HashMap<>();

            List<T> docs = new ArrayList<>();
            
            for (int i = 0; i < docIds.size(); i++) {
                YnsBulkGetResult result = responses.getResults().get(i);
                
                if (result.getDocs().size() > 1) {
                    List<YnsBulkGetResultItem> conflictingDocs = result.getDocs().stream().map(item -> item.get("ok")).toList();
                    
                    conflictingDocumentsMap.computeIfAbsent(result.getDocId(), k -> new ArrayList<>()).addAll((Collection<? extends T>) conflictingDocs);
                } else {
                    if (result.getDocs().get(0).containsKey("ok")) {
                        T doc = (T) result.getDocs().get(0).get("ok");
                        
                        new YnsDocumentAccessor((YnsDocument) doc).setCurrentDb(ynsDb);
                        
                        docs.add(doc);
                    } else {//error
                        YnsBulkGetErrorResultItem errorResult = (YnsBulkGetErrorResultItem) result.getDocs().get(0).get("error");
                        
                        if (!errorResult.getError().equals("not_found")) {
                            errorDocumentsMap.put(errorResult.getDocId(), errorResult);
                        }
                    }
                }
            }

            if (!conflictingDocumentsMap.isEmpty() || !errorDocumentsMap.isEmpty()) {
                //незабыть дописать код
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
                return new YnsResponseHandler<>(response, new TypeReference<YnsBulkGetResponse>() {/* empty */}, transformer, ynsDb.mapper, opInfo, ynsOperationStats).transform();
            } catch (YnsBulkGetRuntimeException e) {
                throw new YnsBulkGetException(e.getConflictingDocumentsMap(), e.getErrorDocumentsMap());
            }
        } finally {
            semaphore.release();
        }
    }
}