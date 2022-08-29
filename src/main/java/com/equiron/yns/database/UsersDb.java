package com.equiron.yns.database;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsDbConfig;
import com.equiron.yns.annotation.model.AnnotationConfigOption;

@Component
@YnsDbConfig(dbName = "_users", selfDiscovering = AnnotationConfigOption.DISABLED)
public class UsersDb extends YnsDb {
    public UsersDb(com.equiron.yns.YnsDbConfig config) {
        super(config);
    }
}
