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
        TestDoc testDoc1 = new TestDoc("1");
        TestDoc testDoc2 = new TestDoc("2");
        TestDoc testDoc3 = new TestDoc("3");
        
        long t1 = System.currentTimeMillis();
        
        Future<List<TestDoc>> res = db.async().saveOrUpdate(testDoc1, testDoc2, testDoc3);
        
        System.out.println("Request time: " + (System.currentTimeMillis() - t1));//Request time: 0
        
        res.get();
        
        Future<List<TestDoc>> docs = db.getTestView().<TestDoc>createDocQuery().async().asDocs(); 
        
        Assertions.assertEquals(3, docs.get().size());
    }
}
