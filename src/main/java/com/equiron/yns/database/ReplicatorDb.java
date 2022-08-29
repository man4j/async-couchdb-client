package com.equiron.yns.database;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsDbConfig;
import com.equiron.yns.annotation.model.AnnotationConfigOption;

@Component
@YnsDbConfig(dbName = "_replicator", selfDiscovering = AnnotationConfigOption.DISABLED)
public class ReplicatorDb extends YnsDb {
    public ReplicatorDb(com.equiron.yns.YnsDbConfig config) {
        super(config);
    }
}
