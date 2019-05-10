package com.equiron.acc.tutorial.lesson4;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;

@Component
public class ExampleDb extends CouchDb {
    public ExampleDb(CouchDbConfig config) {
        super(config);
    }
}
