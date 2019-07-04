package com.equiron.acc.fixture;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;

@Security(admins = @SecurityPattern(names = "admin"), members = @SecurityPattern(roles = "oms"))
public class RemoteTestDb extends CouchDb {
    public RemoteTestDb(CouchDbConfig config) {
        super(config);
    }
}
