package com.equiron.acc.profiler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Striped;

import net.logstash.logback.marker.Markers;


public class CouchDbOperationStats {
    private final ConcurrentHashMap<String, OperationProfile> byTypeAndInfoAndStackTraceMap = new ConcurrentHashMap<>();
    
    private final Striped<Lock> striped = Striped.lock(4096);
    
    private final String dbName;
    
    private static final Logger LOGSTASH_JSON_LOGGER = LoggerFactory.getLogger("LOGSTASH_JSON_LOGGER");
    
    private volatile String logstashHost;
    
    public CouchDbOperationStats(String dbName) {
        this.dbName = dbName;
        
        logstashHost = System.getenv("LOGSTASH_HOST");
        
        if (logstashHost != null && !logstashHost.isBlank()) {        
            Thread t = new Thread() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        for (OperationProfile profile : byTypeAndInfoAndStackTraceMap.values()) {
                            Map<String, Object> metrics = new HashMap<>();
                            
                            metrics.put("acc.database", profile.getDatabase());
                            metrics.put("acc.operationType", profile.getOperationType());
                            metrics.put("acc.operationInfo", profile.getOperationInfo());
                            metrics.put("acc.stacktrace", profile.getStackTrace());
                            metrics.put("acc.totalTime", profile.getTotalTime());
                            metrics.put("acc.count", profile.getCount());
                            metrics.put("acc.size", profile.getSize());
                            metrics.put("acc.avg", profile.getAvgOperationTime());
                            metrics.put("acc.max", profile.getMaxOperationTime());
                            metrics.put("acc.min", profile.getMinOperationTime());
                            
                            info(metrics);
                        }
                        
                        try {
                            Thread.sleep(30_000);
                        } catch (@SuppressWarnings("unused") InterruptedException e) {
                            break;
                        }
                    }
                }
            };
            
            t.setDaemon(true);
            t.start();
        }
    }
    
    public void info(@Nonnull Map<String, Object> fields) {
        fields.put("acc.metrics", true);//чтобы отличать метрики и логи
            
        LOGSTASH_JSON_LOGGER.info(Markers.appendEntries(fields), "");
    }

    public void addOperation(OperationInfo opInfo) {
        if (logstashHost != null && !logstashHost.isBlank()) {      
            if (!dbName.equals("_replicator") && !dbName.equals("_users") && opInfo.getStackTrace() != null) {
                fill(byTypeAndInfoAndStackTraceMap, opInfo, dbName + "." + opInfo.getOperationType() + "." + opInfo.getOperationInfo() + "." + opInfo.getStackTrace().hashCode());
            }
        }
    }
    
    private void fill(ConcurrentHashMap<String, OperationProfile> map, OperationInfo opInfo, String key) {
        striped.get(key).lock();
        
        try {
            OperationProfile prevProfile = map.get(key);
    
            long opTime = System.currentTimeMillis() - opInfo.getStartTime();
            
            if (prevProfile == null) {
                map.put(key, new OperationProfile(dbName, opInfo.getOperationType(), opInfo.getOperationInfo(), opInfo.getStackTrace(), opTime, opInfo.getSize()));
            } else {
                OperationProfile profile = new OperationProfile(dbName, opInfo.getOperationType(), opInfo.getOperationInfo(), opInfo.getStackTrace());
                
                profile.setTotalTime(prevProfile.getTotalTime() + opTime);
                profile.setCount(prevProfile.getCount() + 1);
                profile.setSize(prevProfile.getSize() + opInfo.getSize());
                profile.setAvgOperationTime(profile.getTotalTime() / profile.getCount());
                profile.setMaxOperationTime(Math.max(prevProfile.getMaxOperationTime(), opTime));
                profile.setMinOperationTime(Math.min(prevProfile.getMinOperationTime(), opTime));
                
                map.put(key, profile);
            }
        } finally {
            striped.get(key).unlock();
        }
    }
}
