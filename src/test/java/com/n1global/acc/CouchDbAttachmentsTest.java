package com.n1global.acc;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.fixture.TestDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbBulkResponse;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

public class CouchDbAttachmentsTest {
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
    public void shouldAddAttachmentToExistingDocument() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbDocument doc = new CouchDbDocument();

            db.saveOrUpdate(doc);

            db.attach(doc, in, attachmentName, "image/gif");

            Response r = db.getAttachment(doc.getDocId(), attachmentName);

            Assert.assertEquals(9559, r.getResponseBodyAsBytes().length);
        }
    }

    @Test
    public void shouldCreateDocumentAndAddAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            Response r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assert.assertEquals(9559, r.getResponseBodyAsBytes().length);
        }
    }

    @Test
    public void tryFetchNonExistingAttachment() {
        Response r = db.getAttachment("123", "Non-existing attachment");

        Assert.assertEquals(404, r.getStatusCode());
    }

    @Test
    public void tryDeleteNonExistingAttachment() {
        CouchDbDocument doc = new CouchDbDocument();

        db.saveOrUpdate(doc);

        boolean result = db.deleteAttachment(doc, "Non-existing attachment");

        Assert.assertTrue(result);//why true?
    }

    @Test
    public void deleteAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            Response r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assert.assertEquals(200, r.getStatusCode());

            Assert.assertTrue(db.deleteAttachment(putResponse.getDocIdAndRev(), attachmentName));

            r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assert.assertEquals(404, r.getStatusCode());
        }
    }
}
