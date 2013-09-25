package com.n1global.acc;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.exception.http.CouchDbConflictException;
import com.n1global.acc.fixture.GenericTestDocWrapper;
import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.fixture.TestDoc;
import com.n1global.acc.fixture.TestDocDescendant;
import com.n1global.acc.json.CouchDbDesignDocument;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbDocumentAttachment;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.util.Base64;

public class CouchDbCrudTest {
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
    public void shouldSaveDoc() {
        TestDoc testDoc = new TestDoc();

        db.saveOrUpdate(testDoc);

        Assert.assertFalse(testDoc.getDocId().isEmpty());
        Assert.assertFalse(testDoc.getRev().isEmpty());
    }

    @Test
    public void shouldSaveDocInBatch() {
        TestDoc testDoc = new TestDoc();

        db.saveOrUpdate(testDoc, true);

        Assert.assertFalse(testDoc.getDocId().isEmpty());
        Assert.assertNull(testDoc.getRev());
    }

    @Test
    public void shouldSilentlyRejectUpdateDocInBatch() {
        TestDoc testDoc1 = new TestDoc("doc1");
        testDoc1.setDocId("1");

        TestDoc testDoc2 = new TestDoc("doc2");
        testDoc2.setDocId("1");

        db.saveOrUpdate(testDoc1);
        db.saveOrUpdate(testDoc2, true);

        db.ensureFullCommit();

        TestDoc result = db.get("1");

        Assert.assertTrue(result.getName().equals("doc1"));
    }

    @Test
    public void shouldSaveDocWithAttachment() throws UnsupportedEncodingException {
        TestDoc testDoc = new TestDoc();

        testDoc.addAttachment("qwe", new CouchDbDocumentAttachment("text/plain", Base64.encode("Привет!".getBytes("UTF-8"))));

        db.saveOrUpdate(testDoc);

        testDoc = db.get(testDoc.getDocId());

        Assert.assertTrue(testDoc.getAttachment("qwe").isStub());
    }

    @Test
    public void shouldSaveGenericDoc() {
        GenericTestDocWrapper d = new GenericTestDocWrapper();

        d.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d);

        d = db.get(d.getDocId());

        Assert.assertEquals(BigDecimal.class, d.getValue().getClass());
        Assert.assertEquals(new BigDecimal("123"), d.getValue());
    }

    @Test
    public void shouldConflictWhenUpdate() {
        try {
            db.saveOrUpdate(new CouchDbDocument("1"));
            db.saveOrUpdate(new CouchDbDocument("1"));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof CouchDbConflictException);
        }
    }

    @Test
    public void shouldGetDesignDocs() {
        Assert.assertEquals(1, db.getDesignDocs().size());
    }

    @Test
    public void shouldGetAllDocs() {
        db.saveOrUpdate(new TestDoc());

        Assert.assertEquals(db.getBuiltInView().createQuery().asIds().size(), db.getInfo().getDocCount());
    }

    @Test
    public void shouldDeleteDoc() {
        TestDoc testDoc = db.saveOrUpdate(new TestDoc());

        Assert.assertNotNull(db.get(testDoc.getDocId()));

        Assert.assertTrue(db.delete(testDoc));

        Assert.assertNull(db.get(testDoc.getDocId()));
    }

    @Test
    public void shouldReturnFalseWhenDelete() {
        Assert.assertFalse(db.delete(new CouchDbDocIdAndRev("1", "1")));
    }

    @Test
    public void shouldBulkDelete() {
        TestDoc testDoc = db.saveOrUpdate(new TestDoc());

        Assert.assertNotNull(db.get(testDoc.getDocId()));

        testDoc.setDeleted();

        db.bulk(testDoc);

        Assert.assertNull(db.get(testDoc.getDocId()));
    }

    @Test
    public void shouldBulkSave() {
        List<CouchDbDocument> docs = new ArrayList<>();

        docs.add(new CouchDbDocument());
        docs.add(new CouchDbDocument());

        long count = db.getInfo().getDocCount();

        List<CouchDbDocument> savedDocs = db.bulk(docs);

        Assert.assertEquals(2, savedDocs.size());
        Assert.assertEquals(db.getInfo().getDocCount(), count + 2);
    }

    @Test
    public void shouldCheckBulkConflicts() {
        List<CouchDbDocument> docs = new ArrayList<>();

        docs.add(new CouchDbDocument());
        docs.add(new CouchDbDocument());

        db.bulk(docs);

        for (CouchDbDocument d : docs) {
            Assert.assertFalse(d.isInConflict());

            d.setRev(null);
        }

        db.bulk(docs);

        for (CouchDbDocument d : docs) {
            Assert.assertTrue(d.isInConflict());
        }
    }

    @Test
    public void shouldSaveOneDocWhenDocIdIsSame() {
        List<CouchDbDocument> docs = new ArrayList<>();

        CouchDbDocument d1 = new CouchDbDocument("1");
        CouchDbDocument d2 = new CouchDbDocument("1");

        docs.add(d1);
        docs.add(d2);

        db.bulk(docs);

        Assert.assertFalse(d1.isInConflict());
        Assert.assertTrue(d2.isInConflict()); //not saved
    }

    @Test
    public void shoulWorkCompact() {
        Assert.assertTrue(db.compact());
    }

    @Test
    public void shouldWorkPolymorphicQuery() {
        boolean findDesignDoc = false;
        boolean findTestDocClass = false;
        boolean findTestDocDescendantClass = false;

        db.bulk(new TestDoc(), new TestDocDescendant());

        for (CouchDbDocument d : db.getBuiltInView().createDocQuery().asDocIterator()) {
            if (d.getClass() == CouchDbDesignDocument.class) {
                findDesignDoc = true;
            }

            if (d.getClass() == TestDoc.class) {
                findTestDocClass = true;
            }

            if (d.getClass() == TestDocDescendant.class) {
                findTestDocDescendantClass = true;
            }
        }

        Assert.assertTrue(findDesignDoc);
        Assert.assertTrue(findTestDocClass);
        Assert.assertTrue(findTestDocDescendantClass);
    }

    @Test
    public void shouldInsertDocWithHandler() {
        db.getTestUpdater().update("myDocId");

        Assert.assertEquals("myDocId", db.get("myDocId").getDocId());
    }
}
