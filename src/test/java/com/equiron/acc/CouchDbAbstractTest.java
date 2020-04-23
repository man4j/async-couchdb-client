package com.equiron.acc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.equiron.acc.fixture.TestDb;

public class CouchDbAbstractTest {
    protected TestDb db;

    @BeforeEach
    public void before() {
        db = new TestDb(new CouchDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                   .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                   .setUser(System.getProperty("USER"))
                                                   .setPassword(System.getProperty("PASSWORD"))
                                                   .setSelfDiscovering(true)
                                                   .build());
    }

    @AfterEach
    public void after() {
        db.deleteDb();
    }
}
