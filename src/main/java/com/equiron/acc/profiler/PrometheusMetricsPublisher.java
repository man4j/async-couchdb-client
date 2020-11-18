package com.equiron.acc.profiler;

import java.io.IOException;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusMetricsPublisher {
    private volatile static HTTPServer promServer = null;
    
    static {
        Integer port = System.getenv("COUCHDB_METRICS_PORT") != null ? Integer.parseInt(System.getenv("COUCHDB_METRICS_PORT")) : null;
        
        if (port == null) {
            port = System.getProperty("COUCHDB_METRICS_PORT") != null ? Integer.parseInt(System.getProperty("COUCHDB_METRICS_PORT")) : null;
        }
        
        if (port != null) {
            try {
                promServer = new HTTPServer(port, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private final String dbName;
    
    private static String[] labels = new String[] {"database", "operation_type", "operation_info", "stack_trace"};
    
    private static final Counter count      = Counter.build().name("acc_operation_count").labelNames(labels).help("Operation count").register();
    private static final Counter totalTime  = Counter.build().name("acc_operation_time").labelNames(labels).help("Operation time ms").register();
    private static final Counter size       = Counter.build().name("acc_operation_size").labelNames(labels).help("Operation size").register();
    private static final Counter success    = Counter.build().name("acc_success").labelNames(labels).help("Success count").register();
    private static final Counter notFound   = Counter.build().name("acc_notFound").labelNames(labels).help("Not found count").register();
    private static final Counter conflicts  = Counter.build().name("acc_conflicts").labelNames(labels).help("Conflicts count").register();
    private static final Counter errors     = Counter.build().name("acc_errors").labelNames(labels).help("Errors count").register();
    
    public PrometheusMetricsPublisher(String dbName) {
        this.dbName = dbName;
    }
    
    public void addOperation(OperationInfo opInfo) {
        if (promServer != null) {      
            fill(opInfo);
        }
    }
    
    private void fill(OperationInfo opInfo) {
        long opTime = System.currentTimeMillis() - opInfo.getStartTime();
            
        String[] labels = {dbName, opInfo.getOperationType().toString(), opInfo.getOperationInfo(), opInfo.getStackTrace()};
            
        count.labels(labels).inc();
        totalTime.labels(labels).inc(opTime);
        size.labels(labels).inc(opInfo.getSize());
        
        if (opInfo.getStatus() == 200) {
            success.labels(labels).inc();
        } else if (opInfo.getStatus() == 404) {
            notFound.labels(labels).inc();
        } else if (opInfo.getStatus() == 409) {
            conflicts.labels(labels).inc();
        } else {
            errors.labels(labels).inc();
        }
    }
}
