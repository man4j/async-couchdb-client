package com.equiron.acc;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.equiron.acc.changes.YnsEventHandler;
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.YnsEvent;

public class CouchDbNotificationsTest extends YnsAbstractTest {
    @Test
    public void shouldWork() throws Exception {
        TestDoc testDoc = new TestDoc("qwe");
        
        db.saveOrUpdate(testDoc);
        db.delete(testDoc.getDocIdAndRev());
        
        try (CouchDbEventListener<TestDoc> listener = new CouchDbEventListener<>(db) {/*empty*/};) {        
            final CountDownLatch latch = new CountDownLatch(1);
    
            listener.addEventHandler(new YnsEventHandler<TestDoc>() {
                @Override
                public void onEvent(YnsEvent<TestDoc> event) {
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
            
            System.out.println("OK");
        }
    }
}
