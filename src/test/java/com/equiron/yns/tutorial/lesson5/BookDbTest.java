package com.equiron.yns.tutorial.lesson5;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.yns.YnsAbstractTest;
import com.equiron.yns.YnsDbConfig;

public class BookDbTest {
    private BookDb db;

    @BeforeEach
    public void before() {
        db = new BookDb(new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
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
        db.saveOrUpdate(List.of(new Book("Perl", "oreilly"),
                                new Book("Python", "oreilly"),
                                new Book("JavaScript", "oreilly"),
                                new Book("Spring in Action", "manning"),
                                new Book("JBoss in Action", "manning")));

        Assertions.assertEquals(2, db.getPublishersBooksView().createMapQuery().byKey("manning").asIds().size());
        Assertions.assertEquals(3, db.getPublishersBooksView().createMapQuery().byKey("oreilly").asIds().size());
    }

    @Test
    public void shouldGetBookCountByPublisher() {
        db.saveOrUpdate(List.of(new Book("Perl", "oreilly"),
                                new Book("Python", "oreilly"),
                                new Book("JavaScript", "oreilly"),
                                new Book("Spring in Action", "manning"),
                                new Book("JBoss in Action", "manning")));

        Assertions.assertEquals(2, db.getPublishersBooksView().createReduceQuery().group().byKey("manning").asValue());
        Assertions.assertEquals(3, db.getPublishersBooksView().createReduceQuery().group().byKey("oreilly").asValue());
    }
}
