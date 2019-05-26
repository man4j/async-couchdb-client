package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.CouchDbConfig;

@Component
@CouchDbConfig(dbName = "_replicator", selfDiscovering = false)
public class ReplicatorDb extends CouchDb {
    public ReplicatorDb(com.equiron.acc.CouchDbConfig config) {
        super(config);
    }
}
