package com.equiron.acc;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.exception.YnsBulkDocumentException;
import com.equiron.acc.json.YnsDocument;

public class YnsBulkTest extends YnsAbstractTest {
    @Test
    public void shouldThrowExceptionOnConflict() {
        YnsDocument d1 = new YnsDocument("1");
        YnsDocument d2 = new YnsDocument("1");
        
        try {
            db.saveOrUpdate(List.of(d1, d2));
        } catch (YnsBulkDocumentException ex) {
            ex.getResponses().forEach(resp -> {
                System.out.println("docId: " + resp.getDocId());
                System.out.println("forbidden: " + resp.isForbidden());
                System.out.println("conflict: " + resp.isInConflict());
                System.out.println("error: " + resp.isUnknownError());
            });
        }
        
        Assertions.assertThrows(YnsBulkDocumentException.class, () -> db.saveOrUpdate(List.of(d1, d2)));
    }
    
    @Test
    public void shouldWork() {
        YnsDocument d1 = new YnsDocument("1");
        YnsDocument d2 = new YnsDocument("2");
        
        db.saveOrUpdate(List.of(d1, d2));
  
        Assertions.assertEquals(2, db.get(List.of("1", "2", "3"), YnsDocument.class).size());
    }
}
