package com.equiron.acc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAttachment;
import com.equiron.acc.util.StreamResponse;

public class CouchDbAttachmentsTest extends YnsAbstractTest {
    @Test
    public void shouldUseBuiltInAttachments() {
        YnsDocument doc = new YnsDocument();
        
        doc.getAttachments().put("key", new YnsDocumentAttachment("text/plain", "value"));
        
        db.saveOrUpdate(doc);

        doc = db.get(doc.getDocId(), true);
        
        Assertions.assertEquals("value", doc.getAttachment("key").getTextData());
    }
    
    @Test
    public void shouldAddAttachmentToExistingDocument() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            YnsDocument doc = new YnsDocument();

            db.saveOrUpdate(doc);

            db.attach(doc.getDocIdAndRev(), in, attachmentName, "image/gif");

            byte[] attachment = db.getAttachmentAsBytes(doc.getDocId(), attachmentName);

            Assertions.assertEquals(9559, attachment.length);
        }
    }

    @Test
    public void shouldCreateDocumentAndAddAttachment() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            YnsBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            byte[] attachment = db.getAttachmentAsBytes(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(9559, attachment.length);
        }
    }
    
    @Test
    public void shouldCreateDocumentAndAddAttachmentAndGetAsBytes() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            YnsBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            byte[] data = db.getAttachmentAsBytes(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(9559, data.length);
            
            Files.write(Paths.get("test123.gif"), data);
        }
    }
    
    @Test
    public void shouldCreateDocumentAndAddAttachmentAndGetAsStream() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            YnsBulkResponse putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            StreamResponse response = db.getAttachmentAsStream(putResponse.getDocId(), attachmentName);
            Assertions.assertEquals(9559, Integer.parseInt(response.getHeader("Content-Length")));
            Assertions.assertEquals(9559, IOUtils.toByteArray(response.getStream()).length);
            Assertions.assertEquals(200, response.getStatus());
            
            String etag = response.getHeader("ETag");
            
            response = db.getAttachmentAsStream(putResponse.getDocId(), attachmentName, Map.of("If-None-Match", etag));
            Assertions.assertEquals(0, Integer.parseInt(response.getHeader("Content-Length")));
            Assertions.assertEquals(0, IOUtils.toByteArray(response.getStream()).length);
            Assertions.assertEquals(304, response.getStatus());
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
        YnsDocument doc = new YnsDocument();

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
