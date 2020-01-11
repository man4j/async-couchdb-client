package com.equiron.acc.tutorial.lesson2;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.CouchDbConfig;

public class AutoSuggestTest {
    private SimpleCityDb db;

    @BeforeEach
    public void before() {
        db = new SimpleCityDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                   .setUser("admin")
                                                   .setPassword("root")
                                                   .build());
    }
    
    @AfterEach
    public void after() {
        db.deleteDb();
    }

    @Test
    public void shouldWork() {
        db.saveOrUpdate(new City("Moscow"), new City("London"), new City("Minsk"));

        List<City> cities = db.suggest("M");
        
        Assertions.assertEquals(2, cities.size());
    }
}