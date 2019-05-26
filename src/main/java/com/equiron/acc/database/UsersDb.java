package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.CouchDbConfig;

@Component
@CouchDbConfig(dbName = "_users", selfDiscovering = false)
public class UsersDb extends CouchDb {
    //empty
}
