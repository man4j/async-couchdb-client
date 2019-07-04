package com.equiron.acc;

import java.io.IOException;
import java.util.Collections;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.Test;

import com.equiron.acc.database.UsersDb;
import com.equiron.acc.fixture.ReplicatedTestDb;
import com.equiron.acc.json.security.CouchDbUser;

public class CouchDbReplicationTest {
    @Test
    public void shouldCreateUser() throws IOException {
        try (AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build())) {
            UsersDb db = new UsersDb(new CouchDbConfig.Builder().setIp("139.162.145.223")
                                                                .setPort(5984)
                                                                .setUser("admin")
                                                                .setPassword("PassWord123")
                                                                .setHttpClient(httpClient)
                                                                .build());
            
            db.saveOrUpdate(new CouchDbUser("oms", "123456", Collections.singleton("oms")));
        }
    }
    
    @Test
    public void shouldCreateRemote() throws IOException {
        try (AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build())) {
            ReplicatedTestDb db = new ReplicatedTestDb(new CouchDbConfig.Builder().setIp("139.162.145.223")
                                                                                  .setPort(5984)
                                                                                  .setUser("admin")
                                                                                  .setPassword("PassWord123")
                                                                                  .setHttpClient(httpClient)
                                                                                  .build());
        }
    }
    
    @Test
    public void shouldReplicate() throws IOException {
        try (AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build())) {
            ReplicatedTestDb db = new ReplicatedTestDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                                                  .setPort(15984)
                                                                                  .setUser("admin")
                                                                                  .setPassword("PassWord123")
                                                                                  .setHttpClient(httpClient)
                                                                                  .build());
        }
    }
}
