package com.n1global.acc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.Request;

public class CouchDbFieldAccessor {
    private CouchDb couchDb;

    public CouchDbFieldAccessor(CouchDb couchDb) {
        this.couchDb = couchDb;
    }

    public ObjectMapper getMapper() {
        return couchDb.mapper;
    }

    public Request getPrototype() {
        return couchDb.prototype;
    }
}
