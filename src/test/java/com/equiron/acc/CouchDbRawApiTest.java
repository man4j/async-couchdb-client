package com.equiron.acc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.fixture.TestDb;
import com.equiron.acc.fixture.TestDoc;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class CouchDbRawApiTest {
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
    public void shouldQueryRawDoc() {
        TestDoc testDoc = new TestDoc("Name");

        db.saveOrUpdate(testDoc);

        Map<String, Object> m = db.getBuiltInView().createRawDocQuery().byKey(testDoc.getDocId()).asDoc();

        Assert.assertTrue(m.get("name").equals("Name"));
    }

    @Test
    public void shouldGetRawDoc() {
        TestDoc testDoc = new TestDoc("Name");

        db.saveOrUpdate(testDoc);

        Map<String, Object> m = db.getRaw(testDoc.getDocId());

        Assert.assertTrue(m.get("name").equals("Name"));
    }

    @Test
    public void shouldUpdateRawDoc() {
        TestDoc testDoc = new TestDoc("Name");

        db.saveOrUpdate(testDoc);

        Map<String, Object> m = db.getRaw(testDoc.getDocId());

        m.put("name", "UpdatedName");

        db.saveOrUpdate(m);

        m = db.getRaw(testDoc.getDocId());

        Assert.assertTrue(m.get("name").equals("UpdatedName"));
    }

    @Test
    public void shouldBulkRawDoc() {
        Map<String, Object> doc1 = new HashMap<>();
        Map<String, Object> doc2 = new HashMap<>();

        doc1.put("name", "name1");
        doc2.put("name", "name2");

        db.saveOrUpdate(doc1, doc2);

        List<String> ids = Arrays.asList((String)doc1.get("_id"), (String)doc2.get("_id"));

        Assert.assertEquals(2, db.getBuiltInView().createRawDocQuery().byKeys(ids).asDocs().size());
    }
}
