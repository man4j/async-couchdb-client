package com.n1global.acc;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.equiron.acc.CouchDbConfig;
import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbCrudAsyncTest {
    private TestDb db;

    private AsyncHttpClient httpClient;

    @Before
    public void before() {
        httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
                                                                  .setRequestTimeout(-1)
                                                                  .build());

        db = new TestDb(new CouchDbConfig.Builder().setServerUrl("http://127.0.0.1:5984")
                                                   .setUser("admin")
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
    public void shouldSaveDocAsync() throws InterruptedException, ExecutionException {
        TestDoc testDoc1 = new TestDoc();
        TestDoc testDoc2 = new TestDoc();
        TestDoc testDoc3 = new TestDoc();
        
        long t1 = System.currentTimeMillis();
        
        Future<List<TestDoc>> docs = db.async().saveOrUpdate(testDoc1)
                                               .thenCompose(doc -> db.async().saveOrUpdate(testDoc2))
                                               .thenCompose(doc -> db.async().saveOrUpdate(testDoc3))
                                               .thenCompose(doc -> db.getTestView().<TestDoc>createDocQuery().async().asDocs());
        
        System.out.println("Request time: " + (System.currentTimeMillis() - t1));//Request time: 0
        
        Assert.assertEquals(3, docs.get().size());
    }
}
