package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.TestDoc;

public class CouchDbBulkTest extends CouchDbAbstractTest {
    @Test
    public void shouldSaveInBulk() {
        TestDoc testDoc1 = new TestDoc("new 1");
        TestDoc testDoc2 = new TestDoc("new 2");

        db.saveOrUpdate(testDoc1, testDoc2);
        
        testDoc1.setName("updated 1");
        testDoc2.setName("updated 2");
        
        db.saveOrUpdate(testDoc1, testDoc2);
        
        Assertions.assertEquals("updated 1", db.getBuiltInView().<TestDoc>createDocQuery().byKey(testDoc1.getDocId()).asDoc().getName());
        Assertions.assertEquals("updated 2", db.getBuiltInView().<TestDoc>createDocQuery().byKey(testDoc2.getDocId()).asDoc().getName());
    }
}
