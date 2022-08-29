package com.equiron.yns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.fixture.TestDoc;
import com.equiron.yns.json.YnsDesignInfo;

public class YnsMapViewTest extends YnsAbstractTest {
    @Test
    public void shouldIterateAndRemove() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc("name" +  i));
        }
        
        Assertions.assertEquals(10, db.getByIdErlangView().createQuery().asIds().size());
    }

    @Test
    public void shouldGetViewInfo() {
        YnsDesignInfo designInfo = db.getTestView().getInfo();

        Assertions.assertEquals("test_view", designInfo.getName());
        Assertions.assertEquals("javascript", designInfo.getViewInfo().getLanguage());
    }
}
