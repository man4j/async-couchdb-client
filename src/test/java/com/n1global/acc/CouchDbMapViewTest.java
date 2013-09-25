package com.n1global.acc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.n1global.acc.json.CouchDbDesignInfo;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbMapViewTest {
    private TestDb db;

    private AsyncHttpClient httpClient;

    @Before
    public void before() {
        httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setRequestTimeoutInMs(-1).build());

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
    public void shouldIterateAndRemove() {
        long docCount = db.getInfo().getDocCount();

        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc("name" +  i));
        }

        for (TestDoc doc : db.getTestView().<TestDoc>createDocQuery().asDocIterator(3)) {
            db.delete(doc);
        }

        Assert.assertEquals(docCount, db.getInfo().getDocCount());

        Assert.assertEquals(10, db.getInfo().getDocDelCount());
    }

    @Test
    public void shouldGetViewInfo() {
        CouchDbDesignInfo designInfo = db.getTestView().getInfo();

        Assert.assertEquals("default", designInfo.getName());
        Assert.assertEquals("javascript", designInfo.getViewInfo().getLanguage());
    }
}
