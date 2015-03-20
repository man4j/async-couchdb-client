package com.n1global.acc.tutorial.lesson1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.json.CouchDbDocumentAttachment;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.util.Base64;

public class UserDbTest {
    private UserDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new UserDb(new CouchDbConfig.Builder().setUser("root")
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
    public void shouldSaveAndDelete() {
        User userJohn = new User("John", 25);

        db.saveOrUpdate(userJohn);//simple save

        db.bulk(new User("Ivan", 22), new User("Mary", 18));//bulk save

        Assert.assertEquals(3, db.getInfo().getDocCount());

        db.delete(userJohn); // userJohn has correct assigned docId and revision after saveOrUpdate

        Assert.assertEquals(2, db.getInfo().getDocCount());

        List<User> otherUsers = db.getBuiltInView().<User>createDocQuery().asDocs().stream().filter(d -> d.getClass() == User.class).collect(Collectors.toList());//filter design docs if exists

        for (User doc : otherUsers) {
            doc.setDeleted();
        }

        db.bulk(otherUsers);//bulk delete

        Assert.assertEquals(0, db.getInfo().getDocCount());
    }

    @Test
    public void shouldSaveAndGetAndDelete() {
        String docId = db.saveOrUpdate(new User("John", 25)).getDocId();

        User userJohn = db.get(docId);

        Assert.assertNotNull(userJohn);

        db.delete(userJohn);

        userJohn = db.get(docId);

        Assert.assertNull(userJohn);
    }

    @Test
    public void attachAvatar() throws IOException {
        User userJohn = new User("John", 25);

        db.saveOrUpdate(userJohn);

        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            db.attach(userJohn, in, "avatar", "image/gif");
        }

        Response r = db.getAttachment(userJohn, "avatar");

        Assert.assertEquals(200, r.getStatusCode());
    }

    @Test
    public void attachAvatarAsBase64() throws IOException {
        User userJohn = new User("John", 25);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            FileCopyUtils.copy(in, out);
        }

        userJohn.addAttachment("avatar", new CouchDbDocumentAttachment("image/gif", Base64.encode(out.toByteArray())));

        db.saveOrUpdate(userJohn);

        Response r = db.getAttachment(userJohn, "avatar");

        Assert.assertEquals(200, r.getStatusCode());
    }
}
