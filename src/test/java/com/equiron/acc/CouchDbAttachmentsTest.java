package com.equiron.acc;

import java.io.IOException;
import java.io.InputStream;

import org.asynchttpclient.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDocument;

public class CouchDbAttachmentsTest extends CouchDbAbstractTest {
    @Test
    public void shouldAddAttachmentToExistingDocument() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbDocument doc = new CouchDbDocument();

            db.saveOrUpdate(doc);

            db.attach(doc.getDocIdAndRev(), in, attachmentName, "image/gif");

            Response r = db.getAttachment(doc.getDocId(), attachmentName);

            Assertions.assertEquals(9559, r.getResponseBodyAsBytes().length);
        }
    }

    @Test
    public void shouldCreateDocumentAndAddAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            Response r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(9559, r.getResponseBodyAsBytes().length);
        }
    }

    @Test
    public void tryFetchNonExistingAttachment() {
        Response r = db.getAttachment("123", "Non-existing attachment");

        Assertions.assertEquals(404, r.getStatusCode());
    }

    @Test
    public void tryDeleteNonExistingAttachment() {
        CouchDbDocument doc = new CouchDbDocument();

        db.saveOrUpdate(doc);

        boolean result = db.deleteAttachment(doc.getDocIdAndRev(), "Non-existing attachment");

        Assertions.assertTrue(result);//why true?
    }

    @Test
    public void deleteAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            Response r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(200, r.getStatusCode());

            Assertions.assertTrue(db.deleteAttachment(putResponse.getDocIdAndRev(), attachmentName));

            r = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(404, r.getStatusCode());
        }
    }
}