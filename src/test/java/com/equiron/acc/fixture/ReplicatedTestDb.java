package com.equiron.acc.fixture;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.Replicated;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;

@Replicated(targetIp = "139.162.145.223", targetPort = "5984", targetUser = "oms", targetPassword = "123456")
@Security(admins = @SecurityPattern(names = "admin"), members = @SecurityPattern(roles = "oms"))
public class ReplicatedTestDb extends CouchDb {
    public ReplicatedTestDb(CouchDbConfig config) {
        super(config);
    }
}
 