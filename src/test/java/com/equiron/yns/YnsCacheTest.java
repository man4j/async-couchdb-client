package com.equiron.yns;

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

import com.equiron.yns.fixture.TestDb2;
import com.equiron.yns.fixture.TestDoc;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=YnsCacheTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses=TestDb2.class)
public class YnsCacheTest {
    @Autowired
    private TestDb2 testDb2;
    
    @AfterEach
    public void after() {
        testDb2.deleteDb();
    }
    
    @Bean
    public YnsDbConfig ynsDbConfig() {
        return new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                        .setPort(Integer.parseInt(System.getProperty("PORT")))
                                        .setUser(System.getProperty("USER"))
                                        .setPassword(System.getProperty("PASSWORD"))
                                        .setEnableDocumentCache(true)
                                        .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                        .build();
    }

    @Test
    public void shouldSaveDoc() {
        TestDoc testDoc = new TestDoc();
        
        testDb2.saveOrUpdate(testDoc);
        
        testDoc = testDb2.get(testDoc.getDocId(), TestDoc.class);
        
        Assertions.assertNotNull(testDoc);
    }    
}
