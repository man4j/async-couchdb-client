package com.equiron.acc.tutorial.lesson6;

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

import com.equiron.acc.CouchDbAbstractTest;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.database.UsersDb;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=SyncTest.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@ComponentScan(basePackageClasses= {LocalDb.class, UsersDb.class, ReplicatorDb.class})
public class SyncTest {
    @Autowired
    private LocalDb localDb;
    
    @Autowired
    private RemoteDb remoteDb;
    
    @Autowired
    private UsersDb usersDb;
    
    @Autowired
    private ReplicatorDb replicatorDb;
    
    private CouchDbEventListener<OmsDocument> localListener;
    
    private CouchDbEventListener<OmsDocument> remoteListener;
    
    @AfterEach
    public void after() throws Exception {
        if (localListener != null) {
            localListener.close();
        }
        
        if (remoteListener != null) {
            remoteListener.close();
        }
        
        localDb.deleteDb();
        remoteDb.deleteDb();
        usersDb.deleteDb();
        replicatorDb.deleteDb();
    }
    
    @Bean
    public CouchDbConfig couchDbConfig() {
        return new CouchDbConfig.Builder().setHost(System.getProperty("HOST"))
                                          .setPort(Integer.parseInt(System.getProperty("PORT")))
                                          .setUser(System.getProperty("USER"))
                                          .setPassword(System.getProperty("PASSWORD"))
                                          .setHttpClientProviderType(CouchDbAbstractTest.PROVIDER)
                                          .build();
    }

    @Test
    public void shouldReplicate() throws InterruptedException {
        //Серверная сторона
        registerNewClient();
        startRemoteListener();
        
        //Клиентская сторона
        startLocalListener();
        
        Assertions.assertTrue(localDb.saveOrUpdate(new OmsDocument("doc1", "oms1")).get(0).isOk());
        
        Thread.sleep(10_000);
        
        Assertions.assertNull(localDb.get("doc1"));//документы в локальной базе полностью удалены
    }

    private void startLocalListener() {
        localListener = new CouchDbEventListener<>(localDb) {};

        localListener.addEventHandler(e -> {
            if (!e.isDeleted()) {
                OmsDocument doc = e.getDoc();
                
                if (doc.getStatus() == OmsDocumentStatus.PROCESSED) {
                    Assertions.assertTrue(localDb.delete(doc.getDocIdAndRev()).get(0).isOk()); //удаляем обработанные документы
                }
            }
        });
        
        localListener.startListening("0");
    }

    private void startRemoteListener() {
        remoteListener = new CouchDbEventListener<>(remoteDb) {};
        
        remoteListener.addEventHandler(e -> {
            if (!e.isDeleted()) {
                OmsDocument doc = e.getDoc();
                
                if (doc.getStatus() == OmsDocumentStatus.CREATED) {
                    doc.setStatus(OmsDocumentStatus.PROCESSED);
                    Assertions.assertTrue(remoteDb.saveOrUpdate(doc).get(0).isOk());
                }
            }
        });
        
        remoteListener.startListening("0");
    }

    private void registerNewClient() {
        CustomUser client = new CustomUser("oms1", "123456", Collections.singleton("oms"));
        
        client.setFirstName("first name");
        client.setLastName("last name");
        
        usersDb.saveOrUpdate(client);//регистрируем нового клиента
        Assertions.assertTrue(client.isOk());//Проверка, что все ок
    }
}
