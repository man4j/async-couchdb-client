package com.equiron.acc.tutorial.lesson7;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.YnsAbstractTest;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.changes.YnsLastDbSequenceStorage;

public class ForumDbTest {
    private ForumDb db;
    
    private Indexer indexer;
    
    private Searcher searcher;
    
    @SuppressWarnings("resource")
    @BeforeEach
    public void before() throws Exception {
        db = new ForumDb(new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                  .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                  .setUser(System.getProperty("USER"))
                                                  .setPassword(System.getProperty("PASSWORD"))
                                                  .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                                  .build());
        
        indexer = new Indexer(db, new YnsLastDbSequenceStorage(db));
        searcher = new Searcher(indexer.getIndexWriter());
        indexer.startListening();
    }
    
    @AfterEach
    public void after() throws Exception {
        indexer.close();
        db.deleteDb();
    }

    @Test
    public void shouldIndexAndSearch() throws Exception {
        Topic topic = new Topic("This is cool topic!");
        Message messageAboutCats = new Message("Message about cats", topic.getDocId());
        
        db.saveOrUpdate(List.of(topic, 
                                messageAboutCats, 
                                new Message("Message about cats", topic.getDocId()),
                                new Message("Message about dogs", topic.getDocId())));

        Thread.sleep(5_000);//sleep while indexing our docs.

        Assertions.assertEquals(messageAboutCats.getDocId(), searcher.search("cats").iterator().next());
        Assertions.assertEquals(topic.getDocId(), searcher.search("topic").iterator().next());
    }
}
