package com.equiron.yns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.fixture.TestDoc;
import com.equiron.yns.json.YnsDesignInfo;

public class YnsMapErlangViewTest extends YnsAbstractTest {
    @Test
    public void shouldWork() {
        for (int i = 0; i < 10; i++) {
            db.saveOrUpdate(new TestDoc("name" +  i));
        }
        
        Assertions.assertEquals(10, db.getByIdErlangView().createQuery().asIds().size());
    }

    @Test
    public void shouldGetViewInfo() {
        YnsDesignInfo designInfo = db.getByIdErlangView().getInfo();

        Assertions.assertEquals("by_id_erlang_view", designInfo.getName());
        Assertions.assertEquals("erlang", designInfo.getViewInfo().getLanguage());
    }
}
