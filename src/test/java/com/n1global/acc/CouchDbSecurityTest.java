package com.n1global.acc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.exception.http.CouchDbForbiddenException;
import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbSecurityTest {
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
    
    @Test(expected = CouchDbForbiddenException.class)
    public void shouldPreventUpdateDoc() {
        TestDoc doc = new TestDoc();
        
        doc.setName("Name1");
        
        db.saveOrUpdate(doc);
        
        doc.setName("bomb");
        
        db.saveOrUpdate(doc);
    }
}
