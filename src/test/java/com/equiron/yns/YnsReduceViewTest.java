package com.equiron.yns;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.fixture.TestDoc;

public class YnsReduceViewTest extends YnsAbstractTest {
    @Test
    public void shouldWork() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc());
        }

        int result = db.getReducedTestView().createQuery().asValue();

        Assertions.assertEquals(10, result);
    }

    @Test
    public void shouldWorkGrouping() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc());
        }

        List<Integer> result = db.getReducedTestView().createQuery().group().asValues();

        Assertions.assertEquals(10, result.size());
    }
}