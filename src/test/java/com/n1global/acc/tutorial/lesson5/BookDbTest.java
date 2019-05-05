package com.n1global.acc.tutorial.lesson5;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.equiron.acc.CouchDbConfig;
import com.ning.http.client.AsyncHttpClient;

public class BookDbTest {
    private BookDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new BookDb(new CouchDbConfig.Builder().setUser("admin")
                                                    .setPassword("root")
                                                    .setHttpClient(httpClient)
                                                    .build());
    }

    @After
    public void after() {
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

        Assert.assertEquals(2, db.getPublishersBooksView().createDocQuery().byKey("manning").asDocs().size());
        Assert.assertEquals(3, db.getPublishersBooksView().createDocQuery().byKey("oreilly").asDocs().size());
    }

    @Test
    public void shouldGetBookCountByPublisher() {
        db.saveOrUpdate(new Book("Perl", "oreilly"),
                        new Book("Python", "oreilly"),
                        new Book("JavaScript", "oreilly"),
                        new Book("Spring in Action", "manning"),
                        new Book("JBoss in Action", "manning"));

        Assert.assertEquals(Integer.valueOf(2), db.getPublishersBooksView().createReduceQuery().group().byKey("manning").asValue());
        Assert.assertEquals(Integer.valueOf(3), db.getPublishersBooksView().createReduceQuery().group().byKey("oreilly").asValue());
    }
}
