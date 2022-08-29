package com.equiron.yns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.exception.YnsBulkDocumentException;
import com.equiron.yns.fixture.TestDoc;

public class YnsValidateDocUpdateTest extends YnsAbstractTest {
    @Test
    public void shouldPreventUpdateDoc() {
        TestDoc doc = new TestDoc();
        
        doc.setName("bomb");
        
        Assertions.assertThrows(YnsBulkDocumentException.class, () -> db.saveOrUpdate(doc));
    }
}
