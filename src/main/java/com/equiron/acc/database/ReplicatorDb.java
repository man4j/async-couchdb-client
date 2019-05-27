package com.equiron.acc.database;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.CouchDbConfig;

@CouchDbConfig(dbName = "_replicator", selfDiscovering = false)
public class ReplicatorDb extends CouchDb {
    public ReplicatorDb(com.equiron.acc.CouchDbConfig config) {
        super(config);
    }
}
