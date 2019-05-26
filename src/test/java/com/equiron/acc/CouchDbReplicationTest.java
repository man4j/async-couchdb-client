package com.equiron.acc;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.RemoteTestDb;
import com.equiron.acc.fixture.ReplicatedTestDb;
import com.equiron.acc.fixture.TestDoc;

public class CouchDbReplicationTest {
    @Test
    public void shouldReplicate() throws InterruptedException, IOException {
        try (AsyncHttpClient httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build())) {
            RemoteTestDb remoteDb = new RemoteTestDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                                                .setPort(15984)
                                                                                .setHttpClient(httpClient)
                                                                                .build());
    
            ReplicatedTestDb db = new ReplicatedTestDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                                                  .setPort(5984)
                                                                                  .setHttpClient(httpClient)
                                                                                  .build());
            
            TestDoc doc = new TestDoc("ok");
            
            String docId = db.saveOrUpdate(doc).get(0).getDocId();
            
            Thread.sleep(5_000);
            
            Assertions.assertNotNull(remoteDb.get(docId));
        }
    }
}
