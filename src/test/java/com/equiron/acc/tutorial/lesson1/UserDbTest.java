package com.equiron.acc.tutorial.lesson1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;

import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.json.CouchDbBulkResponse;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAttachment;

public class UserDbTest {
    private UserDb db;

    private AsyncHttpClient httpClient;

    @BeforeEach
    public void before() {
        httpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(-1).build());

        db = new UserDb(new CouchDbConfig.Builder().setServerUrl("http://91.242.38.71:5984")
                                                   .setUser("admin")
                                                   .setPassword("root")
                                                   .setHttpClient(httpClient)
                                                   .build());
    }

    @AfterEach
    public void after() throws IOException {
        db.deleteDb();

        httpClient.close();
    }

    @Test
    public void shouldSaveAndDelete() {
        User userJohn = new User("John", 25);
        
        for (User u : db.saveOrUpdate(userJohn, new User("Ivan", 22), new User("Mary", 18))) {
            Assertions.assertTrue(u.isOk());
        }

        Assertions.assertEquals(3, db.getInfo().getDocCount());

        for (CouchDbBulkResponse res : db.delete(userJohn.getDocIdAndRev())) {
            Assertions.assertTrue(res.isOk());
        }

        Assertions.assertEquals(2, db.getInfo().getDocCount());

        List<User> otherUsers = db.getBuiltInView().<User>createDocQuery().asDocs().stream().filter(d -> d.getClass() == User.class).collect(Collectors.toList());//filter design docs if exists

        for (CouchDbBulkResponse res : db.delete(otherUsers.stream().map(CouchDbDocument::getDocIdAndRev).collect(Collectors.toList()))) {
            Assertions.assertTrue(res.isOk());
        }

        Assertions.assertEquals(0, db.getInfo().getDocCount());
    }

    @Test
    public void attachAvatar() throws IOException {
        User userJohn = new User("John", 25);

        db.saveOrUpdate(userJohn);
        
        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            db.attach(userJohn.getDocIdAndRev(), in, "avatar", "image/gif");
        }

        Response r = db.getAttachment(userJohn.getDocId(), "avatar");

        Assertions.assertEquals(200, r.getStatusCode());
    }

    @Test
    public void attachAvatarAsBase64() throws IOException {
        User userJohn = new User("John", 25);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            FileCopyUtils.copy(in, out);
        }

        userJohn.addAttachment("avatar", new CouchDbDocumentAttachment("image/gif", Base64.getEncoder().encodeToString(out.toByteArray())));

        db.saveOrUpdate(userJohn);

        Response r = db.getAttachment(userJohn.getDocId(), "avatar");

        Assertions.assertEquals(200, r.getStatusCode());
    }
}
