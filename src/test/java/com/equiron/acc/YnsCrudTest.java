package com.equiron.acc;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.GenericTestDoc;
import com.equiron.acc.fixture.GenericTestDocDescendant;
import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.fixture.TestDocDescendant;
import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.core.type.TypeReference;

public class YnsCrudTest extends YnsAbstractTest {
    @Test
    public void shouldSaveDoc() {
        TestDoc testDoc = new TestDoc();
        
        db.saveOrUpdate(testDoc);
        
        testDoc = db.get(testDoc.getDocId(), TestDoc.class);
        
        Assertions.assertNotNull(testDoc);
    }
    
    @Test
    public void shouldWorkGenericDoc() {
        GenericTestDoc<BigDecimal> d1 = new GenericTestDoc<>();

        d1.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d1);

        d1 = db.get(d1.getDocId(), new TypeReference<GenericTestDoc<BigDecimal>>() {/* empty */});

        Assertions.assertEquals(BigDecimal.class, d1.getValue().getClass());
        Assertions.assertEquals(new BigDecimal("123"), d1.getValue());
    }
    
    @Test
    public void shouldWorkGenericQuery() {
        GenericTestDoc<BigDecimal> d = new GenericTestDoc<>();

        d.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d);

        d = db.getByIdGenericView().createQuery().byKey(d.getDocId()).asValue();
        
        Assertions.assertEquals(BigDecimal.class, d.getValue().getClass());
        Assertions.assertEquals(new BigDecimal("123"), d.getValue());
    }
    
    @Test
    public void shouldWorkPolymorphicGenericDoc() {
        GenericTestDoc<BigDecimal> d = new GenericTestDocDescendant<>();

        d.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d);

        d = db.get(d.getDocId(), new TypeReference<GenericTestDoc<BigDecimal>>() {/* empty */});

        Assertions.assertEquals(GenericTestDocDescendant.class, d.getClass());
        Assertions.assertEquals(BigDecimal.class, d.getValue().getClass());
        Assertions.assertEquals(new BigDecimal("123"), d.getValue());
    }
    
    @Test
    public void shouldWorkPolymorphicGenericQuery() {
        GenericTestDoc<BigDecimal> d = new GenericTestDocDescendant<>();

        d.setValue(new BigDecimal("123"));

        db.saveOrUpdate(d);

        d = db.getByIdGenericView().createQuery().byKey(d.getDocId()).asValue();
        
        Assertions.assertEquals(BigDecimal.class, d.getValue().getClass());
        Assertions.assertEquals(new BigDecimal("123"), d.getValue());
    }

    @Test
    public void shouldGetDesignDocs() {
        Assertions.assertEquals(4, db.getDesignDocs().size());
    }

    @Test
    public void shouldGetAllDocs() {
        db.saveOrUpdate(new TestDoc());

        Assertions.assertEquals(db.getBuiltInView().createQuery().asIds().size(), db.getInfo().getDocCount());
    }

    @Test
    public void shouldDeleteDoc() {
        TestDoc testDoc = new TestDoc();
        
        db.saveOrUpdate(testDoc);
        
        testDoc = db.get(testDoc.getDocId(), TestDoc.class);
        
        db.delete(testDoc.getDocIdAndRev());
        
        Assertions.assertNull(db.get(testDoc.getDocId(), TestDoc.class));
    }
    
    @Test
    public void shouldWorkPolymorphicGet() {
        TestDoc testDoc = new TestDoc();
        TestDocDescendant testDocDescendant = new TestDocDescendant();
        
        db.saveOrUpdate(List.of(testDoc, testDocDescendant));

        Assertions.assertEquals(TestDoc.class, db.get(testDoc.getDocId(), TestDoc.class).getClass());
        Assertions.assertEquals(TestDocDescendant.class, db.get(testDocDescendant.getDocId(), TestDoc.class).getClass());
    }
    
    @Test
    public void shouldWorkPolymorphicQuery() {
        TestDocDescendant testDocDescendant = new TestDocDescendant();
        
        db.saveOrUpdate(testDocDescendant);
        
        TestDoc doc = db.getByIdView().createQuery().byKey(testDocDescendant.getDocId()).asValue();

        Assertions.assertEquals(TestDocDescendant.class, doc.getClass());
    }
    
    @Test
    public void shouldWorkPolymorphicQuery2() {
        boolean findTestDocClass = false;
        boolean findTestDocDescendantClass = false;

        db.saveOrUpdate(List.of(new TestDoc(), new TestDocDescendant()));

        for (YnsDocument d : db.getByIdView().createQuery().asValueIterator()) {
            if (d.getClass() == TestDoc.class) {
                findTestDocClass = true;
            }

            if (d.getClass() == TestDocDescendant.class) {
                findTestDocDescendantClass = true;
            }
            
            System.out.println(d.getClass());
        }

        Assertions.assertTrue(findTestDocClass);
        Assertions.assertTrue(findTestDocDescendantClass);
    }
}
