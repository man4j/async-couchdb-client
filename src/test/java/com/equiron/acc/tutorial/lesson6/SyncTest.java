package com.equiron.acc.tutorial.lesson6;

import java.util.Collections;

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
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.database.ReplicatorDb;
import com.equiron.acc.database.UsersDb;
import com.equiron.acc.json.CouchDbReplicationDocument;
import com.equiron.acc.json.security.CouchDbUser;

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
        localListener.close();
        remoteListener.close();
        
        localDb.deleteDb();
        remoteDb.deleteDb();
        
        CouchDbUser oms = usersDb.get(CouchDbUser.COUCHDB_USER_PREFIX + "oms1");
        usersDb.delete(oms.getDocIdAndRev());
        
        CouchDbReplicationDocument toRemote = replicatorDb.get("to_remote");
        CouchDbReplicationDocument fromRemote = replicatorDb.get("from_remote");
        
        replicatorDb.delete(toRemote.getDocIdAndRev(), fromRemote.getDocIdAndRev());
    }
    
    @Bean
    public CouchDbConfig couchDbConfig() {
        AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).setReadTimeout(-1).build());
        
        return new CouchDbConfig.Builder().setServerUrl("http://91.242.38.71:5984")
                                          .setUser("admin")
                                          .setPassword("root")
                                          .setHttpClient(httpClient)
                                          .build();
    }

    @Test
    public void shouldReplicate() throws InterruptedException {
        //Серверная сторона
        registerNewClient();
        startRemoteListener();
        
        //Клиентская сторона
        connectToRemote();
        startLocalListener();
        
        Assertions.assertTrue(localDb.saveOrUpdate(new OmsDocument("oms1")).get(0).isOk());
        
        Thread.sleep(5_000);
        
        Assertions.assertEquals(0, localDb.getInfo().getDocCount());//документы в локальной базе полностью удалены
    }

    private void startLocalListener() {
        localListener = new CouchDbEventListener<>(localDb) {};

        localListener.addEventHandler(e -> {
            if (!e.isDeleted()) {
                OmsDocument doc = e.getDoc();
                
                if (doc.getStatus() == OmsDocumentStatus.PROCESSED) {
                    Assertions.assertTrue(localDb.purge(doc.getDocIdAndRev()).get(doc.getDocId())); //удаляем обработанные документы
                }
            }
        });
        
        localListener.startListening();
    }

    private void connectToRemote() {
        CouchDbReplicationDocument toRemote = new CouchDbReplicationDocument("to_remote", "http://91.242.38.71:5984/" + localDb.getDbName(), "http://oms1:123456@91.242.38.71:5984/" + remoteDb.getDbName(), Collections.singletonMap("omsId", "oms1"));
        CouchDbReplicationDocument fromRemote = new CouchDbReplicationDocument("from_remote", "http://oms1:123456@91.242.38.71:5984/" + remoteDb.getDbName(), "http://91.242.38.71:5984/" + localDb.getDbName(), Collections.singletonMap("omsId", "oms1"));

        replicatorDb.saveOrUpdate(toRemote, fromRemote);
        Assertions.assertTrue(toRemote.isOk());  //Проверка, что все ок
        Assertions.assertTrue(fromRemote.isOk());//Проверка, что все ок
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
        
        remoteListener.startListening();
    }

    private void registerNewClient() {
        CustomUser client = new CustomUser("oms1", "123456", Collections.singleton("OMS"));
        
        client.setFirstName("first name");
        client.setLastName("last name");
        
        usersDb.saveOrUpdate(client);//регистрируем нового клиента
        Assertions.assertTrue(client.isOk());//Проверка, что все ок
    }
}
