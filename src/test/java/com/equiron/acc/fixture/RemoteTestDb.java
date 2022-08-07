package com.equiron.acc.fixture;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.YnsSecurity;
import com.equiron.acc.annotation.YnsSecurityPattern;

@Component
@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"), members = @YnsSecurityPattern(roles = "oms"))
public class RemoteTestDb extends CouchDb {
    //empty
}
