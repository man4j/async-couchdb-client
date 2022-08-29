package com.equiron.yns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.json.YnsDocument;

public class YnsBuiltInViewTest extends YnsAbstractTest {
    @Test
    public void shouldIterateDocs() {
        for (int i = 0; i < 11; i++) {
            db.saveOrUpdate(new YnsDocument(i + ""));
        }

        int docsIterated = 0;

        long docsCount = db.getInfo().getDocCount();

        for (String id : db.getBuiltInView().createQuery().asIdIterator(5)) {
            docsIterated++;
        }

        Assertions.assertEquals(docsCount, docsIterated);
    }
}
