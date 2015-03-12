package com.n1global.acc;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.n1global.acc.json.CouchDbEvent;
import com.n1global.acc.notification.CouchDbEventHandler;
import com.n1global.acc.notification.CouchDbEventListener;
import com.n1global.acc.notification.CouchDbNotificationConfig;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbNotificationsTest {
    private TestDb db;

    private AsyncHttpClient httpClient;

    @Before
    public void before() {
        httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new TestDb(new CouchDbConfig.Builder().setServerUrl("http://127.0.0.1:5984")
                                                   .setUser("root")
                                                   .setPassword("root")
                                                   .setHttpClient(httpClient)
                                                   .build());
    }

    @After
    public void after() {
        db.deleteDb();

        httpClient.close();
    }

    @Test
    public void shouldWork() throws Exception {
        db.saveOrUpdate(new TestDoc());

        try(CouchDbEventListener<TestDoc> listener = new CouchDbEventListener<TestDoc>(db, new CouchDbNotificationConfig.Builder().setIncludeDocs(true).build()) {/*empty*/};) {
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
