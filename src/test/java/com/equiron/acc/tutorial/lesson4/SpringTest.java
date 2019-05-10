package com.equiron.acc.tutorial.lesson4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.equiron.acc.json.CouchDbDocument;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/com/n1global/acc/tutorial/lesson4/example-context.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringTest {
    @Autowired
    private ExampleDb exampleDb;

    @After
    public void after() {
        exampleDb.deleteDb();
    }

    @Test
    public void shouldWork() {
        exampleDb.saveOrUpdate(new CouchDbDocument("Hello!"));

        Assert.assertEquals("Hello!", exampleDb.get("Hello!").getDocId());
    }
}
