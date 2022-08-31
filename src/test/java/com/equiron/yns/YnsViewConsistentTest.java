package com.equiron.yns;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.exception.YnsBulkDocumentException;
import com.equiron.yns.fixture.TestDoc;


public class YnsViewConsistentTest extends YnsAbstractTest {
    @Test
    public void shouldWork() {
        int counter = 0;
        
        while (true) {
            TestDoc testDoc = new TestDoc();
            
            db.saveOrUpdate(testDoc);
    
//            String id = db.getBuiltInView().createQuery().byKey(testDoc.getDocId()).asId();
            testDoc = db.get(testDoc.getDocId(), TestDoc.class);

            if (testDoc == null) {
                System.out.println(counter);
                Assertions.assertFalse(true);
            } else {
                counter++;
            }
        }
    }
    
    @Test
    public void shouldWorkSave() {
        int counter = 0;
        
        while (true) {
            String docId = UUID.randomUUID().toString();

            TestDoc testDoc1 = new TestDoc();
            testDoc1.setName("doc1");
            testDoc1.setDocId(docId);

            TestDoc testDoc2 = new TestDoc();
            testDoc2.setName("doc2");
            testDoc2.setDocId(docId);
            
            try {
                db.saveOrUpdate(testDoc1);
                db.saveOrUpdate(testDoc2);
                
                System.out.println(counter);
                Assertions.assertFalse(true);
            } catch(@SuppressWarnings("unused") YnsBulkDocumentException e) {
                counter++;
            }
        }
    }
}
