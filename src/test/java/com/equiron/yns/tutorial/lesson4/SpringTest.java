package com.equiron.yns.tutorial.lesson4;

import org.junit.jupiter.api.AfterEach;
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
import com.equiron.yns.changes.YnsLastDbSequenceStorage;
import com.equiron.yns.changes.YnsSequenceStorage;
import com.equiron.yns.fixture.TestDoc;

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
    
    @Autowired
    private ExampleListener exampleListener;
    
    @AfterEach
    public void after() {
        exampleDb.deleteDb();
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
    
    @Bean
    public YnsSequenceStorage ynsSequenceStorage(ExampleDb db) {
        return new YnsLastDbSequenceStorage(db);
    }

    @Test
    public void shouldWork() throws InterruptedException {
        exampleDb.saveOrUpdate(new TestDoc("qwe"));
        
        exampleListener.getLatch().await();
        
        exampleListener.stopListening();
    }
}
