package com.n1global.acc.tutorial.lesson4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbConfig;

@Component
public class ExampleDb extends CouchDb {
    @Autowired
    public ExampleDb(CouchDbConfig config) {
        super(config);
    }
}
