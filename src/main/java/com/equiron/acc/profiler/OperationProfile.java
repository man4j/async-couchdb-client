package com.equiron.acc.profiler;

public class OperationProfile {
    private String database;
    private OperationType operationType;
    private String operationInfo;
    private String stackTrace;
    
    private long totalTime;
    private long count;
    private long size;
    
    private int successCount;
    private int notFoundCount;
    private int conflictCount;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    public int getNotFoundCount() {
        return notFoundCount;
    }

    public void setNotFoundCount(int notFoundCount) {
        this.notFoundCount = notFoundCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(int conflictCount) {
        this.conflictCount = conflictCount;
    }
}
