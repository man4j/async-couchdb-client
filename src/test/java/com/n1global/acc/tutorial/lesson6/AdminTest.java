package com.n1global.acc.tutorial.lesson6;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.CouchDbAdmin;
import com.n1global.acc.CouchDbConfig;
import com.ning.http.client.AsyncHttpClient;

public class AdminTest {
    private TestDb1 db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new TestDb1(new CouchDbConfig.Builder().setUser("root")
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
    public void shouldPerformAdminOperations() throws Exception {
        System.out.println(db.getInfo());
        System.out.println(db.getMapView().getInfo().getViewInfo());

        Assert.assertTrue(db.compact());
        Assert.assertTrue(db.compactViews(db.getMapView().getInfo().getName()));
        Assert.assertTrue(db.cleanupViews());

        CouchDbAdmin dbAdmin = new CouchDbAdmin(db.getConfig());

        Assert.assertTrue(dbAdmin.getListDbs().contains(db.getDbName()));

        System.out.println(dbAdmin.getStats());

        System.out.println(dbAdmin.getActiveTasks());
    }

    @Test
    public void shouldDeleteDb() {
        CouchDbAdmin dbAdmin = new CouchDbAdmin(db.getConfig());

        Assert.assertTrue(dbAdmin.getListDbs().contains(db.getDbName()));

        Assert.assertTrue(dbAdmin.deleteDb(db.getDbName()));

        Assert.assertFalse(dbAdmin.getListDbs().contains(db.getDbName()));

        Assert.assertFalse(dbAdmin.deleteDb(db.getDbName()));
    }
}
