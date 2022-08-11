package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.annotation.YnsDbConfig;
import com.equiron.acc.annotation.model.AnnotationConfigOption;

@Component
@YnsDbConfig(dbName = "_users", selfDiscovering = AnnotationConfigOption.DISABLED)
public class UsersDb extends YnsDb {
    public UsersDb(com.equiron.acc.YnsDbConfig config) {
        super(config);
    }
}
