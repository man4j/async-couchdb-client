package com.equiron.acc;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import io.prometheus.client.exporter.HTTPServer;

public class CouchDbOperationStats {
    @SuppressWarnings("unused")
    private static HTTPServer server;
    
    static {
        Integer port = System.getenv("COUCHDB_METRICS_PORT") != null ? Integer.parseInt(System.getenv("COUCHDB_METRICS_PORT")) : null;
        port = System.getProperty("COUCHDB_METRICS_PORT") != null ? Integer.parseInt(System.getProperty("COUCHDB_METRICS_PORT")) : null;
        
        if (port != null) {
            try {
                server = new HTTPServer(port, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private final ConcurrentHashMap<String, OperationProfile> byTypeAndInfoAndStackTraceMap = new ConcurrentHashMap<>();
    
    private final Striped<Lock> striped = Striped.lock(4096);
    
    private final String dbName;
    
    private final String instance;
    
    public CouchDbOperationStats(String dbName) {
        this.dbName = dbName;
        
        instance = System.getenv("SERVICE_NAME") == null ? UUID.randomUUID().toString() : System.getenv("SERVICE_NAME");
    }

    public void addOperation(OperationInfo opInfo) {
        if (!dbName.equals("_replicator") && !dbName.equals("_users") && opInfo.getStackTrace() != null) {
            fill(byTypeAndInfoAndStackTraceMap, opInfo, dbName + "." + opInfo.getOperationType() + "." + opInfo.getOperationInfo() + "." + opInfo.getStackTrace().hashCode());
        }
    }
    
    private void fill(ConcurrentHashMap<String, OperationProfile> map, OperationInfo opInfo, String key) {
        striped.get(key).lock();
        
        try {
            OperationProfile prevProfile = map.get(key);
    
            long opTime = System.currentTimeMillis() - opInfo.getStartTime();
            
            if (prevProfile == null) {
                map.put(key, new OperationProfile(instance, dbName, opInfo.getOperationType(), opInfo.getOperationInfo(), opInfo.getStackTrace(), opTime, opInfo.getSize()));
            } else {
                prevProfile.addTotalTime(opTime);
                prevProfile.incCount();
                prevProfile.addSize(opInfo.getSize());
                prevProfile.setAvgOperationTime(prevProfile.getTotalTime() / prevProfile.getCount());
                prevProfile.setMaxOperationTime(Math.max(prevProfile.getMaxOperationTime(), opTime));
                prevProfile.setMinOperationTime(Math.min(prevProfile.getMinOperationTime(), opTime));
            }
        } finally {
            striped.get(key).unlock();
        }
    }
}
