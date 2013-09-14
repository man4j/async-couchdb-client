package com.n1global.acc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

public class CouchDbBase {
    final CouchDbBaseConfig config;

    /**
     * For json serializing / deserializing. This object is thread-safe.
     */
    final ObjectMapper mapper = new ObjectMapper();

    final Request prototype;

    public CouchDbBase(CouchDbBaseConfig config) {
        this.config = config;

        RequestBuilder builder = new RequestBuilder().setHeader("Content-Type", "application/json; charset=utf-8")
                                                     .setBodyEncoding("UTF-8");

        if (this.config.getUser() != null && this.config.getPassword() != null) {
            Realm realm = new Realm.RealmBuilder()
                                   .setPrincipal(this.config.getUser())
                                   .setPassword(this.config.getPassword())
                                   .setUsePreemptiveAuth(true)
                                   .setScheme(AuthScheme.BASIC)
                                   .build();

            builder.setRealm(realm);
        }

        prototype = builder.build();
    }

    public CouchDbBaseConfig getConfig() {
        return config;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
