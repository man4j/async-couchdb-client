package com.equiron.acc.tutorial.lesson4;

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
    static {
        System.setProperty("DB_NAME", "my_db");
    }
    
    @Autowired
    private ExampleDb exampleDb;
    
    @AfterEach
    public void after() {
        exampleDb.deleteDb();
    }
    
    @Bean
    public CouchDbConfig couchDbConfig() {
        return new CouchDbConfig.Builder().setIp("10.0.76.3")
                                          .setUser("admin")
                                          .setPort(15984)
                                          .setPassword("PassWord123")
                                          .build();
    }

    @Test
    public void shouldWork() {
        exampleDb.saveOrUpdate(new CouchDbDocument("Hello!"));

        Assertions.assertEquals("Hello!", exampleDb.get("Hello!").getDocId());
    }
}
