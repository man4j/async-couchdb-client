package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.CouchDbDesignInfo;

public class CouchDbMapViewTest extends CouchDbAbstractTest {
    @Test
    public void shouldIterateAndRemove() {
        long docCount = db.getInfo().getDocCount();

        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc("name" +  i));
        }

        for (TestDoc doc : db.getTestView().<TestDoc>createDocQuery().asDocIterator(3)) {
            db.delete(doc.getDocIdAndRev());
        }

        Assertions.assertEquals(docCount, db.getInfo().getDocCount());

        Assertions.assertEquals(10, db.getInfo().getDocDelCount());
    }

    @Test
    public void shouldGetViewInfo() {
        CouchDbDesignInfo designInfo = db.getTestView().getInfo();

        Assertions.assertEquals("test_view", designInfo.getName());
        Assertions.assertEquals("javascript", designInfo.getViewInfo().getLanguage());
    }
}
