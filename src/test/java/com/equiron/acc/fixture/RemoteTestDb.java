package com.equiron.acc.fixture;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;

@Component
@Security(admins = @SecurityPattern(names = "admin"), members = @SecurityPattern(roles = "oms"))
public class RemoteTestDb extends CouchDb {
    //empty
}
