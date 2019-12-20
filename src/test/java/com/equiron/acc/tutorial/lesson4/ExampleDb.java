package com.equiron.acc.tutorial.lesson4;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.CouchDbConfig;

@Component
@CouchDbConfig(dbName = "${DB_NAME}")
public class ExampleDb extends CouchDb {
    
}
