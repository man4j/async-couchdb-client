package com.equiron.acc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.equiron.acc.fixture.TestDb;
import com.equiron.acc.provider.HttpClientProviderType;

public class YnsAbstractTest {
    protected TestDb db;
    
    public static final HttpClientProviderType PROVIDER = HttpClientProviderType.JDK;

    @BeforeEach
    public void before() {
        db = new TestDb(new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                 .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                 .setUser(System.getProperty("USER"))
                                                 .setPassword(System.getProperty("PASSWORD"))
                                                 .setHttpClientProviderType(PROVIDER)
                                                 .build());
    }

    @AfterEach
    public void after() {
        db.deleteDb();
    }
}
