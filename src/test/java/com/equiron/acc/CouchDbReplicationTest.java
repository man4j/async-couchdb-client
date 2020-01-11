package com.equiron.acc;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.equiron.acc.database.UsersDb;
import com.equiron.acc.fixture.ReplicatedTestDb;
import com.equiron.acc.json.security.CouchDbUser;

public class CouchDbReplicationTest {
    @Test
    public void shouldCreateUser() throws IOException {
        UsersDb db = new UsersDb(new CouchDbConfig.Builder().setIp("139.162.145.223")
                                                            .setPort(5984)
                                                            .setUser("admin")
                                                            .setPassword("PassWord123")
                                                            .build());
        
        db.saveOrUpdate(new CouchDbUser("oms", "123456", Collections.singleton("oms")));
    }
    
    @Test
    public void shouldCreateRemote() throws IOException {
        ReplicatedTestDb db = new ReplicatedTestDb(new CouchDbConfig.Builder().setIp("139.162.145.223")
                                                                              .setPort(5984)
                                                                              .setUser("admin")
                                                                              .setPassword("PassWord123")
                                                                              .build());
    }
    
    @Test
    public void shouldReplicate() throws IOException {
        ReplicatedTestDb db = new ReplicatedTestDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                                              .setPort(15984)
                                                                              .setUser("admin")
                                                                              .setPassword("PassWord123")
                                                                              .build());
    }
}
