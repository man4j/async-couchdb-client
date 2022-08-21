package com.equiron.acc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.TestDoc;
import com.equiron.acc.json.YnsDesignInfo;

public class CouchDbMapErlangViewTest extends YnsAbstractTest {
    @Test
    public void shouldIterateAndRemove() {
        long docCount = db.getInfo().getDocCount();

        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc("name" +  i));
        }

        for (TestDoc doc : db.getTestErlangView().<TestDoc>createDocQuery().asDocIterator(3)) {
            db.delete(doc.getDocIdAndRev());
        }

        Assertions.assertEquals(docCount, db.getInfo().getDocCount());
        Assertions.assertEquals(10, db.getInfo().getDocDelCount());
    }

    @Test
    public void shouldGetViewInfo() {
        YnsDesignInfo designInfo = db.getTestErlangView().getInfo();

        Assertions.assertEquals("test_erlang_view", designInfo.getName());
        Assertions.assertEquals("erlang", designInfo.getViewInfo().getLanguage());
    }
}
