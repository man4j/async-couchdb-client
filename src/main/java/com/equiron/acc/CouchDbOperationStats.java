package com.equiron.acc;

import java.util.concurrent.ConcurrentHashMap;

public class CouchDbOperationStats {
    private static final ConcurrentHashMap<String, OperationProfile> operationsMap = new ConcurrentHashMap<>();
    
    public static void addOperation(OperationInfo opInfo) {
        OperationProfile profile = operationsMap.putIfAbsent(opInfo.getOperationInfo(), new OperationProfile(opInfo.getOperationType(), opInfo.getOperationInfo(), 1, opInfo.getOperationTime(), opInfo.getOperationTime(), opInfo.getOperationTime(), opInfo.getOperationTime()));
        
        if (profile == null) {
            profile = operationsMap.get(opInfo.getOperationInfo());
        }
        
        long totalTime = profile.getTotalTime() + opInfo.getOperationTime();
        long count = profile.getCount() + 1;
        long avgTime = totalTime / count;
        
        OperationProfile updatedProfile = new OperationProfile(opInfo.getOperationType(), opInfo.getOperationInfo(), count, avgTime, Math.max(profile.getMaxOperationTime(), opInfo.getOperationTime()), Math.min(profile.getMinOperationTime(), opInfo.getOperationTime()), totalTime);
        
        operationsMap.put(opInfo.getOperationInfo(), updatedProfile);
    }
    
    public static ConcurrentHashMap<String, OperationProfile> getOperationsMap() {
        return operationsMap;
    }
}
