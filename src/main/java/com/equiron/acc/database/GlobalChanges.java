package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;

@Component
public class GlobalChanges extends CouchDb {
    public GlobalChanges(CouchDbConfig config) {
        super(new CouchDbConfig.Builder().setDbName("_global_changes")
                                         .setHttpClient(config.getHttpClient())
                                         .setUser(config.getUser())
                                         .setPassword(config.getPassword())
                                         .setServerUrl(config.getServerUrl())
                                         .setSelfDiscovering(false).build());
    }
}
