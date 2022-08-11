package com.equiron.acc;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAccessor;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.profiler.YnsOperationStats;
import com.equiron.acc.provider.HttpClientProvider;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.databind.JavaType;

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
    
    protected int getReplicas() {
        return ynsDb.getClusterInfo() == null ? 1 : ynsDb.getClusterInfo().getN();
    }
    
    @SneakyThrows
    protected <T> T get(String docId, JavaType docType, boolean attachments) {
        if (docId == null || docId.trim().isEmpty()) throw new IllegalStateException("The document id cannot be null or empty");

        UrlBuilder urlBuilder = createUrlBuilder().addPathSegment(docId);
        
        if (attachments) {
            urlBuilder.addQueryParam("attachments", "true");
        }

        Function<T, T> transformer = doc -> {
            if (doc != null && doc instanceof YnsDocument) {
                new YnsDocumentAccessor((YnsDocument) doc).setCurrentDb(ynsDb);
            }

            return doc;
        };
        
        OperationType operationType = attachments ? OperationType.GET_WITH_ATTACHMENT : OperationType.GET;
        
        OperationInfo operationInfo = new OperationInfo(operationType, 1, 0);
        
        semaphore.acquire();
        
        try {
            HttpClientProviderResponse response = httpClient.get(urlBuilder.build(), Map.of("Accept", attachments ? "application/json" : "*/*"));
            
            return new YnsResponseHandler<>(response, docType, transformer, ynsDb.mapper, operationInfo, ynsOperationStats).transform();
        } finally {
            semaphore.release();
        }
    }
}