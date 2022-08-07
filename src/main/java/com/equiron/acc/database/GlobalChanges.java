package com.equiron.acc.database;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.annotation.YnsDbConfig;

@Component
@YnsDbConfig(dbName = "_global_changes", selfDiscovering = false)
public class GlobalChanges extends YnsDb {
    //empty
}
