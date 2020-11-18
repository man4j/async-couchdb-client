package com.equiron.acc.profiler;

public class CouchDbOperationStats {
    private final LogstashMetricsPublisher logstashMetricsPublisher;
    
    private final PrometheusMetricsPublisher prometheusMetricsPublisher;
    
    private final String dbName;
    
    public CouchDbOperationStats(String dbName) {
        this.dbName = dbName;
        logstashMetricsPublisher = new LogstashMetricsPublisher(dbName);
        prometheusMetricsPublisher = new PrometheusMetricsPublisher(dbName);
    }

    public void addOperation(OperationInfo opInfo) {
        if (!dbName.equals("_replicator") && !dbName.equals("_users") && opInfo.getStackTrace() != null) {
            logstashMetricsPublisher.addOperation(opInfo);
            prometheusMetricsPublisher.addOperation(opInfo);
        }
    }
}
