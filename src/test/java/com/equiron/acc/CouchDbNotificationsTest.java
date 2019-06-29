package com.equiron.acc;

import java.util.concurrent.CountDownLatch;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.Test;

import com.equiron.acc.changes.CouchDbEventHandler;
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.CouchDbEvent;

public class CouchDbNotificationsTest extends CouchDbAbstractTest {
    @Test
    public void shouldWork() throws Exception {
        TestDoc testDoc = new TestDoc("qwe");
        
        db.saveOrUpdate(testDoc);
        db.delete(testDoc.getDocIdAndRev());
        
        try(AsyncHttpClient listenerClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).setReadTimeout(-1).build());
            CouchDbEventListener<TestDoc> listener = new CouchDbEventListener<>(db, listenerClient) {/*empty*/};) {
            final CountDownLatch latch = new CountDownLatch(1);

            listener.addEventHandler(new CouchDbEventHandler<TestDoc>() {
                @Override
                public void onEvent(CouchDbEvent<TestDoc> event) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onStart() throws Exception {
                    //empty
                    
                }

                @Override
                public void onCancel() throws Exception {
                    //empty                    
                }
            });

            listener.startListening("0");

            latch.await();
            
            listener.stopListening();
        }
    }
}
