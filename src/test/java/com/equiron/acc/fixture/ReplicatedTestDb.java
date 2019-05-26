package com.equiron.acc.fixture;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.annotation.Replicated;

@Replicated(targetIp = "91.242.38.71", targetPort = 15984, targetDbName = "remote_test_db")
public class ReplicatedTestDb extends CouchDb {
    public ReplicatedTestDb(CouchDbConfig config) {
        super(config);
    }
}
