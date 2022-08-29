package com.equiron.yns;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.equiron.yns.changes.YnsEventListener;
import com.equiron.yns.changes.YnsLastDbSequenceStorage;
import com.equiron.yns.fixture.TestDoc;
import com.equiron.yns.json.YnsEvent;

public class YnsNotificationsTest extends YnsAbstractTest {
    @Test
    public void shouldWork() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        try (YnsEventListener listener = new YnsEventListener(db, new YnsLastDbSequenceStorage(db)) {
            @Override
            public void onEvent(YnsEvent event) throws Exception {
                System.out.println("Is new: " + event.isNew());
                System.out.println("Is deleted: " + event.isDeleted());
                
                TestDoc d = db.get(event.getDocId(), TestDoc.class);
                System.out.println(d.getName());
                
                latch.countDown();
            }
        }) {
            listener.startListening();

            db.saveOrUpdate(new TestDoc("qwe"));
            
            latch.await();
        }
    }
}
