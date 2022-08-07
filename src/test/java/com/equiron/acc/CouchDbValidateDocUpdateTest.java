package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.exception.http.YnsForbiddenException;
import com.equiron.acc.fixture.TestDoc;

public class CouchDbValidateDocUpdateTest extends CouchDbAbstractTest {
    @Test
    public void shouldPreventUpdateDoc() {
        TestDoc doc = new TestDoc();
        
        doc.setName("Name1");
        
        db.saveOrUpdate(doc);
        
        Assertions.assertFalse(doc.isForbidden());
        
        doc.setName("bomb");
        
        Assertions.assertThrows(YnsForbiddenException.class, () -> db.saveOrUpdate(doc));
    }
}
