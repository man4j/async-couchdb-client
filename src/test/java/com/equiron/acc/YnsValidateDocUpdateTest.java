package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.exception.YnsBulkDocumentException;
import com.equiron.acc.fixture.TestDoc;

public class YnsValidateDocUpdateTest extends YnsAbstractTest {
    @Test
    public void shouldPreventUpdateDoc() {
        TestDoc doc = new TestDoc();
        
        doc.setName("bomb");
        
        Assertions.assertThrows(YnsBulkDocumentException.class, () -> db.saveOrUpdate(doc));
    }
}
