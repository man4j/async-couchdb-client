package com.equiron.acc.tutorial.lesson2;

import java.io.IOException;
import java.util.List;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.CouchDbConfig;

public class AutoSuggestTest {
    private SimpleCityDb db;

    private AsyncHttpClient httpClient;

    @BeforeEach
    public void before() {
        httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new SimpleCityDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
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
    public void shouldWork() {
        db.saveOrUpdate(new City("Moscow"), new City("London"), new City("Minsk"));

        List<City> cities = db.suggest("M");
        
        Assertions.assertEquals(2, cities.size());
    }
}