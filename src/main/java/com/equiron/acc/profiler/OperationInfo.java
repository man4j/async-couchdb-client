package com.equiron.acc.profiler;

public class OperationInfo {
    private final OperationType operationType;
    
    private final String operationInfo;
    
    private final long startTime;

    private final String stackTrace;
    
    private volatile long docsCount;
    
    private volatile long size;
    
    private volatile int status;
    
    public OperationInfo(OperationType operationType, long docsCount, long size) {
        this(operationType, "", docsCount, size);
    }
    
    public OperationInfo(OperationType operationType, String operationInfo, long docsCount, long size) {
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.startTime = System.currentTimeMillis();
        this.stackTrace = generateStackTrace();
        this.docsCount = docsCount;
        this.size = size;
    }
    
    private static String generateStackTrace() {
        String stackTrace = "";
        
        if (!CouchDbOperationStats.COUCHDB_METRICS_STACK_TRACE) {
            return stackTrace;
        }
        
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (!e.toString().startsWith("java")
             && !e.toString().startsWith("javax")
             && !e.toString().startsWith("jdk")
             && !e.toString().contains("$Proxy") 
             && !e.toString().startsWith("com.equiron.acc") 
             && !e.toString().startsWith("org.springframework")
             && !e.toString().startsWith("org.jboss")
             && !e.toString().startsWith("io.undertow")) {
                stackTrace += e.getClassName().substring(e.getClassName().lastIndexOf(".") + 1) + "." + e.getMethodName() + ":" + e.getLineNumber() + "\n";
            }
            
            if (e.toString().startsWith("com.equiron.acc.CouchDb.init")) {
                return null;
            }
        }
        
        stackTrace = stackTrace.replace("EnhancerBySpringCGLIB", "");
        stackTrace = stackTrace.replace("FastClassBySpringCGLIB", "");
        
        return stackTrace;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getOperationInfo() {
        return operationInfo;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }

    public long getDocsCount() {
        return docsCount;
    }
    
    public void setDocsCount(long docsCount) {
        this.docsCount = docsCount;
    }

    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }    
}
