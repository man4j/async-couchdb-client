package com.equiron.acc.tutorial.lesson4;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.json.CouchDbDocument;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=SpringTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses=ExampleDb.class)
public class SpringTest {
    @Autowired
    private ExampleDb exampleDb;
    
    @AfterEach
    public void after() {
        exampleDb.deleteDb();
    }
    
    @Bean
    public CouchDbConfig couchDbConfig() {
        AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());
        
        return new CouchDbConfig.Builder().setIp("91.242.38.71")
                                          .setUser("admin")
                                          .setPassword("root")
                                          .setHttpClient(httpClient)
                                          .build();
    }

    @Test
    public void shouldWork() {
        exampleDb.saveOrUpdate(new CouchDbDocument("Hello!"));

        Assertions.assertEquals("Hello!", exampleDb.get("Hello!").getDocId());
    }
}
