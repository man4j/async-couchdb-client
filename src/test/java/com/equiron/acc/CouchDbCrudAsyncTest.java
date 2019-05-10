package com.equiron.acc;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.equiron.acc.fixture.TestDoc;

public class CouchDbCrudAsyncTest extends CouchDbAbstractTest {
    @Test
    public void shouldSaveDocAsync() throws InterruptedException, ExecutionException {
        TestDoc testDoc1 = new TestDoc();
        TestDoc testDoc2 = new TestDoc();
        TestDoc testDoc3 = new TestDoc();
        
        long t1 = System.currentTimeMillis();
        
        Future<List<TestDoc>> docs = db.async().saveOrUpdate(testDoc1)
                                               .thenCompose(res -> db.async().saveOrUpdate(testDoc2))
                                               .thenCompose(res -> db.async().saveOrUpdate(testDoc3))
                                               .thenCompose(res -> db.getTestView().<TestDoc>createDocQuery().async().asDocs());
        
        System.out.println("Request time: " + (System.currentTimeMillis() - t1));//Request time: 0
        
        Assertions.assertEquals(3, docs.get().size());
    }
}
