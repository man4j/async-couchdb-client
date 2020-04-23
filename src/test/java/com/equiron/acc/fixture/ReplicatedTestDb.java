package com.equiron.acc.fixture;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.Replicated;

@Component
@Replicated(targetHost = "${HOST}", targetPort = "${PORT}", targetUser = "oms", targetPassword = "123456", targetDbName = "remote_test_db")
public class ReplicatedTestDb extends CouchDb {
    //empty
}
 