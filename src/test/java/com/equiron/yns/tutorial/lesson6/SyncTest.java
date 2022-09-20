package com.equiron.yns.tutorial.lesson6;

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

import com.equiron.yns.YnsAbstractTest;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.changes.YnsEventListener;
import com.equiron.yns.changes.YnsLastDbSequenceStorage;
import com.equiron.yns.database.ReplicatorDb;
import com.equiron.yns.database.UsersDb;
import com.equiron.yns.json.YnsEvent;

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
    
    private YnsEventListener localListener;
    
    private YnsEventListener remoteListener;
    
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
    public YnsDbConfig ynsDbConfig() {
        return new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                        .setPort(Integer.parseInt(System.getProperty("PORT")))
                                        .setUser(System.getProperty("USER"))
                                        .setPassword(System.getProperty("PASSWORD"))
                                        .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                        .build();
    }

    @Test
    public void shouldReplicate() throws InterruptedException {
        //Серверная сторона
        registerNewClient();
        startRemoteListener();
        
        //Клиентская сторона
        startLocalListener();
        
        localDb.saveOrUpdate(new OmsDocument("doc1", "oms1"));
        
        Thread.sleep(10_000);
        
        Assertions.assertNull(localDb.get("doc1", OmsDocument.class));//документы в локальной базе полностью удалены
    }

    private void startLocalListener() {
        localListener = new YnsEventListener(localDb, new YnsLastDbSequenceStorage(localDb)) {
            @Override
            public void onEvent(YnsEvent e) throws Exception {
                if (!e.isDeleted()) {
                    OmsDocument doc = localDb.get(e.getDocId(), OmsDocument.class);
                    
                    if (doc.getStatus() == OmsDocumentStatus.PROCESSED) {
                        localDb.delete(doc.getDocIdAndRev()); //удаляем обработанные документы
                    }
                }                
            }
        };

        localListener.startListening();
    }

    private void startRemoteListener() {
        remoteListener = new YnsEventListener(remoteDb, new YnsLastDbSequenceStorage(usersDb)) {
            @Override
            public void onEvent(YnsEvent e) throws Exception {
                if (!e.isDeleted()) {
                    OmsDocument doc = remoteDb.get(e.getDocId(), OmsDocument.class);
                    
                    if (doc.getStatus() == OmsDocumentStatus.CREATED) {
                        doc.setStatus(OmsDocumentStatus.PROCESSED);
                        remoteDb.saveOrUpdate(doc);
                    }
                }
            }
        };
        
        remoteListener.startListening();
    }

    private void registerNewClient() {
        CustomUser client = new CustomUser("oms1", "123456", Collections.singleton("oms"));
        
        client.setFirstName("first name");
        client.setLastName("last name");
        
        usersDb.saveOrUpdate(client);//регистрируем нового клиента
    }
}
