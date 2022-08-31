package com.equiron.yns.profiler;

import lombok.Getter;
import lombok.Setter;

public class OperationProfile {
    @Getter
    private String database;
    
    @Getter
    private OperationType operationType;
    
    @Getter
    private String operationInfo;
    
    @Getter
    private String stackTrace;
    
    @Getter
    @Setter
    private long totalTime;

    @Getter
    @Setter
    private long count;

    @Getter
    @Setter
    private long docsCount;

    @Getter
    @Setter
    private long size;

    @Getter
    @Setter
    private int status_200;

    @Getter
    @Setter
    private int status_201;
    
    @Getter
    @Setter
    private int status_202;

    @Getter
    @Setter
    private int status_304;

    @Getter
    @Setter
    private int status_other;

    public OperationProfile(String database, OperationType operationType, String operationInfo, String stackTrace) {
        this.database = database;
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.stackTrace = stackTrace;
    }
    
    public OperationProfile(String database, OperationType operationType, String operationInfo, String stackTrace, long time, long size, long docsCount) {
        this.database = database;
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.stackTrace = stackTrace;
        this.docsCount = docsCount;
        
        setCount(1);
        setTotalTime(time);
        setSize(size);
    }
}
