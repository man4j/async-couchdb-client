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
    private long size;

    @Getter
    @Setter
    private int successCount;

    @Getter
    @Setter
    private int notFoundCount;

    @Getter
    @Setter
    private int conflictCount;

    @Getter
    @Setter
    private int errorsCount;
    
    public OperationProfile(String database, OperationType operationType, String operationInfo, String stackTrace) {
        this.database = database;
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.stackTrace = stackTrace;
    }
    
    public OperationProfile(String database, OperationType operationType, String operationInfo, String stackTrace, long time, long size) {
        this.database = database;
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.stackTrace = stackTrace;
        
        setCount(1);
        setTotalTime(time);
        setSize(size);
    }
}
