package com.equiron.acc.profiler;

public class YnsOperationStats {
    public static boolean COUCHDB_METRICS_STACK_TRACE = true;
    public static boolean COUCHDB_METRICS_ENABLE = true;
    
    static {
        if ("false".equalsIgnoreCase(System.getenv("COUCHDB_METRICS_STACK_TRACE"))) {
            COUCHDB_METRICS_STACK_TRACE = false;
        }
        
        if ("false".equalsIgnoreCase(System.getenv("COUCHDB_METRICS_ENABLE"))) {
            COUCHDB_METRICS_ENABLE = false;
        }
    }
    
    private volatile LogstashMetricsPublisher logstashMetricsPublisher;
    
    private final String dbName;
    
    public YnsOperationStats(String dbName) {
        this.dbName = dbName;
        
        if (YnsOperationStats.COUCHDB_METRICS_ENABLE) {
            logstashMetricsPublisher = new LogstashMetricsPublisher(dbName);
        }
    }

    public void addOperation(OperationInfo opInfo) {
        if (!YnsOperationStats.COUCHDB_METRICS_ENABLE) {
            return;
        }
        
        if (!dbName.equals("_replicator") && !dbName.equals("_users") && opInfo.getStackTrace() != null) {
            logstashMetricsPublisher.addOperation(opInfo);
        }
    }
}
