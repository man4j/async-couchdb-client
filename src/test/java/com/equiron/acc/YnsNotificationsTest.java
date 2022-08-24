package com.equiron.acc;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.equiron.acc.changes.YnsEventListener;
import com.equiron.acc.changes.YnsLastDbSequenceStorage;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.YnsEvent;

public class YnsNotificationsTest extends YnsAbstractTest {
    @Test
    public void shouldWork() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        try (YnsEventListener listener = new YnsEventListener(db, new YnsLastDbSequenceStorage(db)) {
            @Override
            public void onEvent(YnsEvent event) throws Exception {
                System.out.println("Is new: " + event.isNew());
                System.out.println("Is deleted: " + event.isDeleted());
                
                latch.countDown();
            }
        }) {
            listener.startListening();

            db.saveOrUpdate(new TestDoc("qwe"));
            
            latch.await();
        }
    }
}
