package com.equiron.acc;

public class OperationInfo {
    private final OperationType operationType;
    
    private final String operationInfo;
    
    private final long startTime;
    
    public OperationInfo(OperationType operationType, String operationInfo) {
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.startTime = System.currentTimeMillis();
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getOperationInfo() {
        return operationInfo;
    }
    
    public long getOperationTime() {
        return System.currentTimeMillis() - startTime;
    }
}
