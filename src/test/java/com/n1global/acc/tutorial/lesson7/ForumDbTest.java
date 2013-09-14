package com.n1global.acc.tutorial.lesson7;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.notification.document.CouchDbDocumentListener;
import com.n1global.acc.notification.document.CouchDbDocumentListenerConfig;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class ForumDbTest {
    private ForumDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    private AsyncHttpClient notificationHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs(-1)
                                                                                                            .setIOThreadMultiplier(1)
                                                                                                            .build());

    private CouchDbDocumentListener listener;

    private Indexer indexer;

    @Before
    public void before() throws Exception {
        db = new ForumDb(new CouchDbConfig.Builder().setUser("root")
                                                    .setPassword("root")
                                                    .setHttpClient(httpClient)
                                                    .build());

        listener = new CouchDbDocumentListener(db, ForumContent.class, new CouchDbDocumentListenerConfig.Builder().setHttpClient(notificationHttpClient).build());

        indexer = new Indexer();

        listener.addDocumentUpdateHandler(indexer);

        listener.startListening();
    }

    @After
    public void after() throws Exception {
        listener.close();

        notificationHttpClient.close();

        db.deleteDb();

        httpClient.close();
    }

    @Test
    public void shouldIndexing() throws Exception {
        Topic topic = db.saveOrUpdate(new Topic("This is cool topic!"));

        Message catsMessage = db.saveOrUpdate(new Message("Message about cats", topic.getDocId()));

        db.bulk(new Message("Message about cars", topic.getDocId()),
                new Message("Message about dogs", topic.getDocId()));

        Thread.sleep(1000);//sleep while indexing our docs.

        Assert.assertEquals(catsMessage.getDocId(), indexer.search("cats").iterator().next());
        Assert.assertEquals(topic.getDocId(), indexer.search("topic").iterator().next());
    }
}
