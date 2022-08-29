package com.equiron.yns;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.json.YnsDocument;
import com.equiron.yns.util.StreamResponse;

public class YnsAttachmentsTest extends YnsAbstractTest {    
    @Test
    public void shouldAddAttachmentToExistingDocument() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            YnsDocument doc = new YnsDocument();

            db.saveOrUpdate(doc);
            db.attach(doc.getDocIdAndRev(), in, attachmentName, "image/gif");

            StreamResponse response = db.getAttachment(doc.getDocId(), attachmentName);
            
            Assertions.assertEquals(9559, Integer.parseInt(response.getHeader("Content-Length")));
            Assertions.assertEquals(9559, response.asBytes().length);
            Assertions.assertEquals(200, response.getStatus());
            
            String etag = response.getHeader("ETag");
            response = db.getAttachment(doc.getDocId(), attachmentName, Map.of("If-None-Match", etag));
            Assertions.assertEquals(0, Integer.parseInt(response.getHeader("Content-Length")));
            Assertions.assertEquals(0, response.asBytes().length);
            Assertions.assertEquals(304, response.getStatus());
        }
    }


    @Test
    public void tryFetchNonExistingAttachment() {
        var response = db.getAttachment("123", "Non-existing attachment");

        Assertions.assertNull(response);
    }

    @Test
    public void tryDeleteNonExistingAttachment() {
        YnsDocument doc = new YnsDocument();

        db.saveOrUpdate(doc);

        boolean result = db.deleteAttachment(doc.getDocIdAndRev(), "Non-existing attachment");

        Assertions.assertFalse(result);
    }

    @Test
    public void deleteAttachment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            String attachmentName = "the/rabbit/pic";

            var putResponse = db.attach("the/doc/id", in, attachmentName, "image/gif");

            var getResponse = db.getAttachment(putResponse.getDocId(), attachmentName);

            Assertions.assertEquals(200, getResponse.getStatus());

            Assertions.assertTrue(db.deleteAttachment(putResponse.getDocIdAndRev(), attachmentName));

            getResponse = db.getAttachment(putResponse.getDocId(), attachmentName);
            
            Assertions.assertNull(getResponse);
        }
    }
}
