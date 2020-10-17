package com.equiron.acc;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class OperationProfile {
    private final String instance;
    
    private final String database;
    
    private final OperationType operationType;
    
    private final String operationInfo;
    
    private final String stackTrace;
    
    private volatile long _maxOperationTime;
    private volatile long _minOperationTime;
    private volatile long _totalTime;
    private volatile long _count;
    
    private static String[] labels = new String[] {"instance", "database", "operation_type", "operation_info", "stack_trace"};
    
    private static Counter count = Counter.build().name("operation_count").labelNames(labels).help("Total operation count").register();
    private static Gauge avgOperationTime = Gauge.build().name("operation_time_avg_ms").labelNames(labels).help("Operation AVG time ms").register();
    private static Gauge maxOperationTime = Gauge.build().name("operation_time_max_ms").labelNames(labels).help("Operation MAX time ms").register();
    private static Gauge minOperationTime = Gauge.build().name("operation_time_min_ms").labelNames(labels).help("Operation MIN time ms").register();
    private static Counter totalTime = Counter.build().name("operation_time_total_ms").labelNames(labels).help("Operation total time ms").register();
    private static Counter size = Counter.build().name("operation_size").labelNames(labels).help("Operation size").register();
    
    public OperationProfile(String instance, String database, OperationType operationType, String operationInfo, String stackTrace, long time, long size) {
        this.instance = instance;
        this.database = database;
        this.operationType = operationType;
        this.operationInfo = operationInfo;
        this.stackTrace = stackTrace;
        
        incCount();
        setAvgOperationTime(time);
        setMaxOperationTime(time);
        setMinOperationTime(time);
        addTotalTime(time);
        addSize(size);
    }
    
    public void incCount() {
        count.labels(instance, database, operationType.toString(), operationInfo, stackTrace).inc();
        _count++;
    }
    
    public void setAvgOperationTime(long amount) {
        avgOperationTime.labels(instance, database, operationType.toString(), operationInfo, stackTrace).set(amount);
    }
    
    public long getMaxOperationTime() {
        return _maxOperationTime;
    }

    public void setMaxOperationTime(long amount) {
        maxOperationTime.labels(instance, database, operationType.toString(), operationInfo, stackTrace).set(amount);
        _maxOperationTime = amount;
    }
    
    public long getMinOperationTime() {
        return _minOperationTime;
    }

    public void setMinOperationTime(long amount) {
        minOperationTime.labels(instance, database, operationType.toString(), operationInfo, stackTrace).set(amount);
        _minOperationTime = amount;
    }
    
    public void addTotalTime(long amount) {
        totalTime.labels(instance, database, operationType.toString(), operationInfo, stackTrace).inc(amount);
        _totalTime += amount;
    }
    
    public void addSize(long amount) {
        size.labels(instance, database, operationType.toString(), operationInfo, stackTrace).inc(amount);
    }
    
    public long getTotalTime() {
        return _totalTime;
    }
    
    public long getCount() {
        return _count;
    }
}
