package com.equiron.acc;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.equiron.acc.fixture.TestDb;

public class CouchDbAbstractTest {
    protected TestDb db;

    protected AsyncHttpClient httpClient;

    @BeforeEach
    public void before() {
        httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new TestDb(new CouchDbConfig.Builder().setServerUrl("http://91.242.38.71:5984")
                                                   //.setUser("admin")
                                                   //.setPassword("root")
                                                   .setHttpClient(httpClient)
                                                   .build());
    }

    @AfterEach
    public void after() throws IOException {
        db.deleteDb();

        httpClient.close();
    }
}
