package com.equiron.yns.tutorial.lesson1;

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

import com.equiron.yns.YnsAbstractTest;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.json.YnsDocument;
import com.equiron.yns.migration.YnsMigration;
import com.equiron.yns.migration.YnsMigrationDb;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=MigrationTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses= {ExampleDb.class, YnsMigration.class})
public class MigrationTest {
    @Autowired
    private ExampleDb exampleDb;
    
    @Autowired
    private YnsMigrationDb ynsMigrationDb;
    
    @AfterEach
    public void after() {
        exampleDb.deleteDb();
        ynsMigrationDb.deleteDb();
    }
    
    @Bean
    public YnsDbConfig ynsDbConfig() {
        return new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                        .setPort(Integer.parseInt(System.getProperty("PORT")))
                                        .setUser(System.getProperty("USER"))
                                        .setPassword(System.getProperty("PASSWORD"))
                                        .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                        .build();
    }
    
    @Test
    public void shouldWork() {
        Assertions.assertNotNull(exampleDb.get("firstDoc", YnsDocument.class));
        Assertions.assertNotNull(exampleDb.get("secondDoc", YnsDocument.class));
    }
}
