package com.equiron.acc.tutorial.lesson7;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.changes.CouchDbEventListener;

public class ForumDbTest {
    private ForumDb db;
    
    private AsyncHttpClient httpClient;
    
    private CouchDbEventListener<ForumContent> listener;

    @BeforeEach
    public void before() throws Exception {
        httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new ForumDb(new CouchDbConfig.Builder().setIp("91.242.38.71")
                                                    .setUser("admin")
                                                    .setPassword("root")
                                                    .setHttpClient(httpClient)
                                                    .build());
        
        listener = new CouchDbEventListener<>(db) { /* empty */};

        indexer = new Indexer();

        listener.addEventHandler(indexer);

        listener.startListening();

    }
    
    @AfterEach
    public void after() throws Exception {
        listener.close();
        db.deleteDb();
        httpClient.close();
    }

    private Indexer indexer;

    @Test
    public void shouldIndexing() throws Exception {
        Topic topic = new Topic("This is cool topic!");
        Message messageAboutCats = new Message("Message about cats", topic.getDocId());
        
        db.saveOrUpdate(topic, 
                        messageAboutCats, 
                        new Message("Message about cars", topic.getDocId()),
                        new Message("Message about dogs", topic.getDocId()));

        Thread.sleep(5_000);//sleep while indexing our docs.

        Assertions.assertEquals(messageAboutCats.getDocId(), indexer.search("cats").iterator().next());
        Assertions.assertEquals(topic.getDocId(), indexer.search("topic").iterator().next());
    }
}
