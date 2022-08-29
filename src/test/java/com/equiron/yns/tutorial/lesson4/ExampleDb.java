package com.equiron.yns.tutorial.lesson4;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsDbConfig;

@Component
@YnsDbConfig(dbName = "${DB_NAME}")
public class ExampleDb extends YnsDb {
    //empty
}
