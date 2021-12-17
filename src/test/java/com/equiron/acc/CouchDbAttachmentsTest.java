package com.equiron.acc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAttachment;

public class CouchDbAttachmentsTest extends CouchDbAbstractTest {
    @Test
    public void shouldUseBuiltInAttachments() {
        CouchDbDocument doc = new CouchDbDocument();
        
        doc.getAttachments().put("key", new CouchDbDocumentAttachment("text/plain", "value"));
        
        db.saveOrUpdate(doc);

        doc = db.get(doc.getDocId(), true);
        
        Assertions.assertEquals("value", doc.getAttachment("key").getTextData());
    }
    
    @Test
    public void shouldAddAttachmentToExistingDocument() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbDocument doc = new CouchDbDocument();

            db.saveOrUpdate(doc);

            db.attach(doc.getDocIdAndRev(), in, attachmentName, "image/gif");

            byte[] attachment = db.getAttachmentAsBytes(doc.getDocId(), attachmentName);

            Assertions.assertEquals(9559, attachment.length);
        }
    }

    @Test
    public void shouldCreateDocumentAndAddAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            byte[] attachment = db.getAttachmentAsBytes(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(9559, attachment.length);
        }
    }
    
    @Test
    public void shouldCreateDocumentAndAddAttachmentAndGetAsBytes() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            byte[] data = db.getAttachmentAsBytes(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(9559, data.length);
            
            Files.write(Paths.get("test123.gif"), data);
        }
    }

    @Test
    public void tryFetchNonExistingAttachment() {
        String attachment = db.getAttachmentAsString("123", "Non-existing attachment");

        Assertions.assertNull(attachment);
    }
    
    @Test
    public void tryFetchNonExistingAttachmentAsString() {
        String attachment = db.getAttachmentAsString("123", "Non-existing attachment");
        
        Assertions.assertNull(attachment);
    }

    @Test
    public void tryDeleteNonExistingAttachment() {
        CouchDbDocument doc = new CouchDbDocument();

        db.saveOrUpdate(doc);

        boolean result = db.deleteAttachment(doc.getDocIdAndRev(), "Non-existing attachment");

        Assertions.assertFalse(result);//why true?
    }

//    @Test
//    public void deleteAttachment() throws IOException {
//        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
//            String attachmentName = "the/rabbit/pic";
//
//            CouchDbBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");
//
//            HttpResponse<byte[]> r = db.getAttachmentAsString(putResponse.getDocId(), attachmentName).getBytes();
//
//            Assertions.assertEquals(200, r.statusCode());
//
//            Assertions.assertTrue(db.deleteAttachment(putResponse.getDocIdAndRev(), attachmentName));
//
//            r = db.getAttachment(putResponse.getDocId(), attachmentName);
//
//            Assertions.assertEquals(404, r.statusCode());
//        }
//    }
}
