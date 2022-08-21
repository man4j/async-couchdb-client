package com.equiron.acc;

import java.util.Collections;

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

import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.database.UsersDb;
import com.equiron.acc.fixture.RemoteTestDb;
import com.equiron.acc.fixture.ReplicatedTestDb;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.security.YnsUser;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=CouchDbReplicationTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses={ReplicatedTestDb.class, UsersDb.class})
public class CouchDbReplicationTest {
    @Autowired
    private UsersDb usersDb;
    
    @Autowired
    private ReplicatorDb replicatorDb;

    @Autowired
    private ReplicatedTestDb replicatedDb;

    @Autowired
    private RemoteTestDb remoteDb;
    
    @Bean
    public CouchDbConfig couchDbConfig() {
        return new CouchDbConfig.Builder().setHost(System.getProperty("HOST"))
                                          .setPort(Integer.parseInt(System.getProperty("PORT")))
                                          .setUser(System.getProperty("USER"))
                                          .setPassword(System.getProperty("PASSWORD"))
                                          .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                          .build();
    }
    
    @Test
    public void shouldReplicate() throws InterruptedException {
        usersDb.saveOrUpdate(new YnsUser("oms", "123456", Collections.singleton("oms")));
        
        Assertions.assertNull(remoteDb.get("1"));
        
        replicatedDb.saveOrUpdate(new YnsDocument("1"));
        
        Thread.sleep(10_000);
        
        Assertions.assertNotNull(remoteDb.get("1"));
    }
    
    @AfterEach
    public void after() {
        usersDb.deleteDb();
        replicatedDb.deleteDb();
        remoteDb.deleteDb();
        replicatorDb.deleteDb();
    }
}
