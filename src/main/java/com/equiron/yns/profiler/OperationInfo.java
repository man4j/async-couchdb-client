package com.equiron.yns.profiler;

import lombok.Getter;
import lombok.Setter;

public class OperationInfo {
    @Getter
    private final OperationType operationType;
    
    @Getter
    private final String operationInfo;
    
    @Getter
    private final long startTime;

    @Getter
    private final String stackTrace;

    @Getter
    @Setter
    private volatile long docsCount;

    @Getter
    @Setter
    private volatile long size;

    @Getter
    @Setter
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
        
        if (!YnsOperationStats.YNS_METRICS_STACK_TRACE) {
            return stackTrace;
        }
        
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (!e.toString().startsWith("java")
             && !e.toString().startsWith("javax")
             && !e.toString().startsWith("jdk")
             && !e.toString().contains("$Proxy") 
             && !e.toString().startsWith("com.equiron.yns") 
             && !e.toString().startsWith("org.springframework")
             && !e.toString().startsWith("org.jboss")
             && !e.toString().startsWith("io.undertow")) {
                stackTrace += "  " + e.getClassName().substring(e.getClassName().lastIndexOf(".") + 1) + "." + e.getMethodName() + ":" + e.getLineNumber() + "\n";
            }
            
            if (e.toString().contains("YnsDb.init")) {
                return null;
            }
        }
        
        stackTrace = stackTrace.replace("EnhancerBySpringCGLIB", "");
        stackTrace = stackTrace.replace("FastClassBySpringCGLIB", "");
        
        return stackTrace;
    }
}
