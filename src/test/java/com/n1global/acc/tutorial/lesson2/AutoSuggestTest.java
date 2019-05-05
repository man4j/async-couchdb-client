package com.n1global.acc.tutorial.lesson2;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.equiron.acc.CouchDbConfig;
import com.ning.http.client.AsyncHttpClient;

public class AutoSuggestTest {
    private SimpleCityDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new SimpleCityDb(new CouchDbConfig.Builder().setUser("admin")
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
        db.saveOrUpdate(new City("Moscow"), new City("London"), new City("Minsk"));

        Assertions.assertThat(db.suggest("M"))
                  .extracting("name")
                  .containsSequence("Minsk", "Moscow");
    }
}