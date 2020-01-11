package com.equiron.acc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.equiron.acc.fixture.TestDb;

public class CouchDbAbstractTest {
    protected TestDb db;

    @BeforeEach
    public void before() {
        db = new TestDb(new CouchDbConfig.Builder().setIp("167.71.63.9")
                                                   .setPort(5984)
                                                   .setUser("admin")
                                                   .setPassword("password")
                                                   .setSelfDiscovering(true)
                                                   .build());
    }

    @AfterEach
    public void after() {
        db.deleteDb();
    }
}
