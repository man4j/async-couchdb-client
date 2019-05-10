package com.equiron.acc;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.equiron.acc.changes.CouchDbEventHandler;
import com.equiron.acc.changes.CouchDbEventListener;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.CouchDbEvent;

public class CouchDbNotificationsTest extends CouchDbAbstractTest {
    @Test
    public void shouldWork() throws Exception {
        db.saveOrUpdate(new TestDoc());

        try(CouchDbEventListener<TestDoc> listener = new CouchDbEventListener<TestDoc>(db) {/*empty*/};) {
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
            });

            listener.startListening();

            latch.await();
        }
    }
}
