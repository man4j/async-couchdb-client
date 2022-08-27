package com.equiron.acc.profiler;

public class YnsOperationStats {
    public static boolean YNS_METRICS_STACK_TRACE = true;
    public static boolean YNS_METRICS_ENABLE = true;
    
    static {
        if ("false".equalsIgnoreCase(System.getenv("YNS_METRICS_STACK_TRACE"))) {
            YNS_METRICS_STACK_TRACE = false;
        }
        
        if ("false".equalsIgnoreCase(System.getenv("YNS_METRICS_ENABLE"))) {
            YNS_METRICS_ENABLE = false;
        }
    }
    
    private volatile LogstashMetricsPublisher logstashMetricsPublisher;
    
    private final String dbName;
    
    public YnsOperationStats(String dbName) {
        this.dbName = dbName;
        
        if (YnsOperationStats.YNS_METRICS_ENABLE) {
            logstashMetricsPublisher = new LogstashMetricsPublisher(dbName);
        }
    }

    public void addOperation(OperationInfo opInfo) {
        if (!YnsOperationStats.YNS_METRICS_ENABLE) {
            return;
        }
        
        if (!dbName.equals("_replicator") && !dbName.equals("_users") && opInfo.getStackTrace() != null) {
            logstashMetricsPublisher.addOperation(opInfo);
        }
    }
}
