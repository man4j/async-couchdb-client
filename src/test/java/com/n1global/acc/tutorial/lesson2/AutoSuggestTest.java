package com.n1global.acc.tutorial.lesson2;

import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.CouchDbConfig;
import com.ning.http.client.AsyncHttpClient;

public class AutoSuggestTest {
    private SimpleCitiesDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new SimpleCitiesDb(new CouchDbConfig.Builder().setUser("root")
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
    public void shouldWork() {
        db.bulk(new City("Moscow"), new City("London"), new City("Minsk"));

        Assertions.assertThat(db.suggest("M"))
                  .onProperty("name")
                  .containsSequence("Minsk", "Moscow");
    }
}