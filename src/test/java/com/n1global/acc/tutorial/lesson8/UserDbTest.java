package com.n1global.acc.tutorial.lesson8;

import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.json.CouchDbDocument;
import com.ning.http.client.AsyncHttpClient;

public class UserDbTest {
    private UserDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new UserDb(new CouchDbConfig.Builder().setUser("admin")
                                                    .setPassword("root")
                                                    .setHttpClient(httpClient)
                                                    .build());
    }

    @After
    public void after() {
        db.deleteDb();

        httpClient.close();
    }

    @Test
    public void shouldUpdateAllDocs() {
        User user1 = new User("John Smith");
        User user2 = new User("Adam Fox");
        User user3 = new User("Amanda Black");

        db.bulk(user1, user2, user3);

        for (CouchDbDocument user : db.getBuiltInView().createDocQuery().asDocs().stream().filter(d -> d.getClass() == User.class).collect(Collectors.toList())) {
            db.getTestUpdater().update(user.getDocId());
        }

        Assert.assertEquals("John",   db.<User>get(user1.getDocId()).getName());
        Assert.assertEquals("Adam",   db.<User>get(user2.getDocId()).getName());
        Assert.assertEquals("Amanda", db.<User>get(user3.getDocId()).getName());
    }
}
