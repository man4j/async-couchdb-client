package com.equiron.acc;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.GenericTestDocWrapper;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.fixture.TestDocDescendant;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDesignDocument;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAttachment;

public class CouchDbCrudTest extends CouchDbAbstractTest {
    @Test
    public void shouldSaveDoc() {
        TestDoc testDoc = new TestDoc();

        db.saveOrUpdate(testDoc);

        Assertions.assertFalse(testDoc.getDocId().isEmpty());
        Assertions.assertFalse(testDoc.getRev().isEmpty());
    }

    @Test
    public void shouldSaveDocWithAttachment() throws UnsupportedEncodingException {
        TestDoc testDoc = new TestDoc();

        testDoc.addAttachment("qwe", new CouchDbDocumentAttachment("text/plain", Base64.getEncoder().encodeToString("Привет!".getBytes("UTF-8"))));

        db.saveOrUpdate(testDoc);

        testDoc = db.getBuiltInView().<TestDoc>createDocQuery().byKey(testDoc.getDocId()).asDoc();

        Assertions.assertTrue(testDoc.getAttachment("qwe").isStub());
    }

    @Test
    public void shouldSaveGenericDoc() {
        GenericTestDocWrapper d = new GenericTestDocWrapper();

        d.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d);

        d = db.getBuiltInView().<GenericTestDocWrapper>createDocQuery().byKey(d.getDocId()).asDoc();

        Assertions.assertEquals(BigDecimal.class, d.getValue().getClass());
        Assertions.assertEquals(new BigDecimal("123"), d.getValue());
    }


    @Test
    public void shouldGetDesignDocs() {
        Assertions.assertEquals(3, db.getDesignDocs().size());
    }

    @Test
    public void shouldGetAllDocs() {
        db.saveOrUpdate(new TestDoc());

        Assertions.assertEquals(db.getBuiltInView().createQuery().asIds().size(), db.getInfo().getDocCount());
    }

    @Test
    public void shouldDeleteDoc() {
        List<TestDoc> response = db.saveOrUpdate(new TestDoc());
        
        String docId = response.get(0).getDocId();

        TestDoc testDoc = db.getBuiltInView().<TestDoc>createDocQuery().byKey(docId).asDoc();
        
        List<CouchDbBulkResponse> deleteResponse = db.delete(testDoc.getDocIdAndRev());
        
        Assertions.assertFalse(deleteResponse.get(0).isInConflict());
        Assertions.assertEquals("", deleteResponse.get(0).getError());
        Assertions.assertEquals("", deleteResponse.get(0).getConflictReason());
        
        Assertions.assertEquals(0, db.getBuiltInView().<TestDoc>createDocQuery().byKey(docId).asDocs().size());
    }
    
    @Test
    public void shouldPurgeDoc() {
        List<TestDoc> response = db.saveOrUpdate(new TestDoc());
        
        String docId = response.get(0).getDocId();

        TestDoc testDoc = db.getBuiltInView().<TestDoc>createDocQuery().byKey(docId).asDoc();
        
        testDoc.setName("qweqwe");
        
        db.saveOrUpdate(testDoc);
        
        Map<String, Boolean> purgeResponse = db.purge(testDoc.getDocIdAndRev());
        Assertions.assertTrue(purgeResponse.get(testDoc.getDocId()));
        
        purgeResponse = db.purge(new CouchDbDocIdAndRev("qwe", "2-1fa02a0db59fe257fa879cc7f69d6672"));
        Assertions.assertFalse(purgeResponse.get("qwe"));
        
        Assertions.assertEquals(0, db.getBuiltInView().<TestDoc>createDocQuery().byKey(docId).asDocs().size());
    }

    @Test
    public void shouldSaveOneDocWhenDocIdIsSame() {
        CouchDbDocument d1 = new CouchDbDocument("1");
        CouchDbDocument d2 = new CouchDbDocument("1");

        db.saveOrUpdate(d1, d2);

        Assertions.assertFalse(d1.isInConflict());
        Assertions.assertTrue(d2.isInConflict()); //not saved
    }

    @Test
    public void shouldWorkPolymorphicQuery() {
        boolean findDesignDoc = false;
        boolean findTestDocClass = false;
        boolean findTestDocDescendantClass = false;

        db.saveOrUpdate(new TestDoc(), new TestDocDescendant());

        for (CouchDbDocument d : db.getBuiltInView().createDocQuery().asDocIterator()) {
            if (d.getClass() == CouchDbDesignDocument.class) {
                findDesignDoc = true;
            }

            if (d.getClass() == TestDoc.class) {
                findTestDocClass = true;
            }

            if (d.getClass() == TestDocDescendant.class) {
                findTestDocDescendantClass = true;
            }
        }

        Assertions.assertTrue(findDesignDoc);
        Assertions.assertTrue(findTestDocClass);
        Assertions.assertTrue(findTestDocDescendantClass);
    }
}
