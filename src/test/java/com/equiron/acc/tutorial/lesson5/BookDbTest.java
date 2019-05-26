package com.equiron.acc.tutorial.lesson5;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.CouchDbConfig;

public class BookDbTest {
    private BookDb db;

    private AsyncHttpClient httpClient;

    @BeforeEach
    public void before() {
        httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new BookDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                   .setUser("admin")
                                                   .setPassword("root")
                                                   .setHttpClient(httpClient)
                                                   .build());
    }
    
    @AfterEach
    public void after() throws IOException {
        db.deleteDb();

        httpClient.close();
    }

    @Test
    public void shouldGetBookByPublisher() {
        db.saveOrUpdate(new Book("Perl", "oreilly"),
                        new Book("Python", "oreilly"),
                        new Book("JavaScript", "oreilly"),
                        new Book("Spring in Action", "manning"),
                        new Book("JBoss in Action", "manning"));

        Assertions.assertEquals(2, db.getPublishersBooksView().createDocQuery().byKey("manning").asDocs().size());
        Assertions.assertEquals(3, db.getPublishersBooksView().createDocQuery().byKey("oreilly").asDocs().size());
    }

    @Test
    public void shouldGetBookCountByPublisher() {
        db.saveOrUpdate(new Book("Perl", "oreilly"),
                        new Book("Python", "oreilly"),
                        new Book("JavaScript", "oreilly"),
                        new Book("Spring in Action", "manning"),
                        new Book("JBoss in Action", "manning"));

        Assertions.assertEquals(Integer.valueOf(2), db.getPublishersBooksView().createReduceQuery().group().byKey("manning").asValue());
        Assertions.assertEquals(Integer.valueOf(3), db.getPublishersBooksView().createReduceQuery().group().byKey("oreilly").asValue());
    }
}
