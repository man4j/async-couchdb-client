package com.n1global.acc;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.equiron.acc.CouchDbConfig;
import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbReduceViewTest {
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
    public void shouldWork() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc());
        }

        int result = db.getReducedTestView().createQuery().asValue();

        Assert.assertEquals(10, result);
    }

    @Test
    public void shouldWorkGrouping() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc());
        }

        List<Integer> result = db.getReducedTestView().createQuery().group().asValues();

        Assert.assertEquals(10, result.size());
    }
}
