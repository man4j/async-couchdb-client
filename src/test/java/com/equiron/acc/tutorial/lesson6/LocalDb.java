package com.equiron.acc.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;

@Component
public class LocalDb extends CouchDb {
    public LocalDb(CouchDbConfig config) {
        super(config);
    }
}
