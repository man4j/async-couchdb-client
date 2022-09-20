package com.equiron.yns;

import java.util.Set;

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

import com.equiron.yns.database.UsersDb;
import com.equiron.yns.exception.YnsBulkDocumentException;
import com.equiron.yns.fixture.ProtectedDb;
import com.equiron.yns.fixture.TestDoc;
import com.equiron.yns.json.security.YnsUser;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=ReadOnlyTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses= {ProtectedDb.class, UsersDb.class})
public class ReadOnlyTest {
    @Autowired
    private ProtectedDb protectedDb;
    
    @Autowired
    private UsersDb usersDb;
    
    @AfterEach
    public void after() {
        protectedDb.deleteDb();
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
    public void tryWrite() {
        try {
            usersDb.saveOrUpdate(new YnsUser("replicator", "password", Set.of("replicator")));
        } catch (Exception e) {
            //empty
        }
        
        try {
            usersDb.saveOrUpdate(new YnsUser("reader", "password", Set.of("reader")));
        } catch (Exception e) {
            //empty
        }
        
        YnsDbConfig replicatorConfig = new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                                .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                                .setUser("replicator")
                                                                .setPassword("password")
                                                                .setSelfDiscovering(false)
                                                                .build();
        
        ProtectedDb suReplicator = new ProtectedDb(replicatorConfig);
        
        suReplicator.saveOrUpdate(new TestDoc("qwe"));
        
        Assertions.assertThrows(YnsBulkDocumentException.class, () -> protectedDb.saveOrUpdate(new TestDoc("qwe")));
    }
}
