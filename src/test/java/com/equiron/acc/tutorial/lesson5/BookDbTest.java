package com.equiron.acc.tutorial.lesson5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.YnsAbstractTest;
import com.equiron.acc.CouchDbConfig;

public class BookDbTest {
    private BookDb db;

    @BeforeEach
    public void before() {
        db = new BookDb(new CouchDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                   .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                   .setUser(System.getProperty("USER"))
                                                   .setPassword(System.getProperty("PASSWORD"))
                                                   .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                                   .build());
    }
    
    @AfterEach
    public void after() {
        db.deleteDb();
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
