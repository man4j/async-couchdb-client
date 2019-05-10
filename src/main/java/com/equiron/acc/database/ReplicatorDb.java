package com.equiron.acc.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;

@Component
public class ReplicatorDb extends CouchDb {
    @Autowired
    public ReplicatorDb(CouchDbConfig config) {
        super(new CouchDbConfig.Builder().setDbName("_replicator")
                                         .setHttpClient(config.getHttpClient())
                                         .setUser(config.getUser())
                                         .setPassword(config.getPassword())
                                         .setServerUrl(config.getServerUrl())
                                         .setSelfDiscovering(false).build());
    }
}
