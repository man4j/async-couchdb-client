package com.equiron.acc.tutorial.lesson2;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.YnsAbstractTest;
import com.equiron.acc.YnsDbConfig;

public class AutoSuggestTest {
    private SimpleCityDb db;

    @BeforeEach
    public void before() {
        db = new SimpleCityDb(new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
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
    public void shouldWork() {
        db.saveOrUpdate(List.of(new City("Moscow"), new City("London"), new City("Minsk")));

        List<City> cities = db.suggest("M");
        
        Assertions.assertEquals(2, cities.size());
    }
}