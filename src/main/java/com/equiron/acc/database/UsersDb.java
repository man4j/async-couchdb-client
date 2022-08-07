package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.annotation.YnsDbConfig;

@Component
@YnsDbConfig(dbName = "_users", selfDiscovering = false)
public class UsersDb extends YnsDb {
    public UsersDb(com.equiron.acc.YnsDbConfig config) {
        super(config);
    }
}
