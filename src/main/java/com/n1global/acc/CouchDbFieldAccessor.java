package com.n1global.acc;

import org.asynchttpclient.Request;

import com.fasterxml.jackson.databind.ObjectMapper;

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
