package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.annotation.YnsDbConfig;

@Component
@YnsDbConfig(dbName = "_replicator", selfDiscovering = false)
public class ReplicatorDb extends YnsDb {
    public ReplicatorDb(com.equiron.acc.YnsDbConfig config) {
        super(config);
    }
}
