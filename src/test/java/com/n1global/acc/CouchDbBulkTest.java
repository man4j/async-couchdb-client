package com.n1global.acc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbBulkTest {
    private TestDb db;

    private AsyncHttpClient httpClient;

    @Before
    public void before() {
        httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

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
    public void shouldSaveInBulk() {
        TestDoc testDoc1 = new TestDoc("new 1");
        TestDoc testDoc2 = new TestDoc("new 2");

        db.saveOrUpdate(testDoc1, testDoc2);
        
        testDoc1.setName("updated 1");
        testDoc2.setName("updated 2");
        
        db.saveOrUpdate(testDoc1, testDoc2);
        
        Assert.assertEquals("updated 1", db.<TestDoc>get(testDoc1.getDocId()).getName());
        Assert.assertEquals("updated 2", db.<TestDoc>get(testDoc2.getDocId()).getName());
    }
}
