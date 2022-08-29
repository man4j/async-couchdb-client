package com.equiron.yns.database;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsDbConfig;
import com.equiron.yns.annotation.model.AnnotationConfigOption;

@Component
@YnsDbConfig(dbName = "_global_changes", selfDiscovering = AnnotationConfigOption.DISABLED)
public class GlobalChanges extends YnsDb {
    public GlobalChanges(com.equiron.yns.YnsDbConfig config) {
        super(config);
    }
}
