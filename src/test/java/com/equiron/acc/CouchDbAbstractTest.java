package com.equiron.acc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.equiron.acc.fixture.TestDb;

public class CouchDbAbstractTest {
    protected TestDb db;

    @BeforeEach
    public void before() {
        db = new TestDb(new CouchDbConfig.Builder().setIp("10.0.64.31")
                                                   .setPort(15984)
                                                   .setUser("admin")
                                                   .setPassword("PassWord123")
                                                   .setSelfDiscovering(true)
                                                   .build());
    }

    @AfterEach
    public void after() {
        db.deleteDb();
    }
}
