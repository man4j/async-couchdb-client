package com.equiron.acc.tutorial.lesson4;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.annotation.YnsDbConfig;

@Component
@YnsDbConfig(dbName = "${DB_NAME}")
public class ExampleDb extends YnsDb {
    //empty
}
