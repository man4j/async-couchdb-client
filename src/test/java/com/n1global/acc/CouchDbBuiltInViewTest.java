package com.n1global.acc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.json.CouchDbDocument;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbBuiltInViewTest {
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
    public void shouldIterateDocs() {
        for (int i = 0; i < 11; i++) {
            db.saveOrUpdate(new CouchDbDocument(i + ""));
        }

        int docsIterated = 0;

        long docsCount = db.getInfo().getDocCount();

        for (CouchDbDocument d : db.getBuiltInView().createDocQuery().asDocIterator(5)) {
            Assert.assertFalse(d.getDocId().isEmpty());

            docsIterated++;
        }

        Assert.assertEquals(docsCount, docsIterated);
    }
}
