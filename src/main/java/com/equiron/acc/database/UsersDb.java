package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;

@Component
public class UsersDb extends CouchDb {
    public UsersDb(CouchDbConfig config) {
        super(new CouchDbConfig.Builder().setDbName("_users")
                                         .setHttpClient(config.getHttpClient())
                                         .setUser(config.getUser())
                                         .setPassword(config.getPassword())
                                         .setServerUrl(config.getServerUrl())
                                         .setSelfDiscovering(false).build());
    }
}
