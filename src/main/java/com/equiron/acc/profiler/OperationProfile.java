package com.equiron.acc.profiler;

public class OperationProfile {
    private String database;
    private OperationType operationType;
    private String operationInfo;
    private String stackTrace;
    private long maxOperationTime;
    private long avgOperationTime;
    private long minOperationTime;
    private long totalTime;
    private long count;
    private long size;
    
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
        setAvgOperationTime(time);
        setMaxOperationTime(time);
        setMinOperationTime(time);
        setTotalTime(time);
        setSize(size);
    }
    
    public void setAvgOperationTime(long amount) {
        avgOperationTime = amount;
    }
    
    public long getMaxOperationTime() {
        return maxOperationTime;
    }

    public void setMaxOperationTime(long amount) {
        maxOperationTime = amount;
    }
    
    public long getMinOperationTime() {
        return minOperationTime;
    }

    public void setMinOperationTime(long amount) {
        minOperationTime = amount;
    }
    
    public long getTotalTime() {
        return totalTime;
    }
    
    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }

    public String getDatabase() {
        return database;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getOperationInfo() {
        return operationInfo;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public long getAvgOperationTime() {
        return avgOperationTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
