package com.equiron.yns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.yns.fixture.TestDoc;

public class YnsRawApiTest extends YnsAbstractTest {
    @Test
    public void shouldQueryRawDoc() {
        TestDoc testDoc = new TestDoc("Name");

        db.saveOrUpdate(testDoc);

        Map<String, Object> m = db.getRaw(testDoc.getDocId());

        Assertions.assertTrue(m.get("name").equals("Name"));
    }

    @Test
    public void shouldBulkRawDoc() {
        Map<String, Object> doc1 = new HashMap<>();
        Map<String, Object> doc2 = new HashMap<>();

        doc1.put("name", "name1");
        doc2.put("name", "name2");

        db.saveOrUpdateRaw(List.of(doc1, doc2));

        Assertions.assertEquals(2, db.getBuiltInView().createQuery()
                                                      .byKeys((String)doc1.get("_id"), (String)doc2.get("_id"))
                                                      .asValues()
                                                      .size());
    }
}
