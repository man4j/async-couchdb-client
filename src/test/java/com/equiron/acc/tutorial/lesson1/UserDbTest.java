package com.equiron.acc.tutorial.lesson1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;

import com.equiron.acc.CouchDbAbstractTest;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.json.YnsBulkResponse;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.YnsDocumentAttachment;

public class UserDbTest {
    private UserDb db;

    @BeforeEach
    public void before() {
        db = new UserDb(new CouchDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                   .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                   .setUser(System.getProperty("USER"))
                                                   .setPassword(System.getProperty("PASSWORD"))
                                                   .setHttpClientProviderType(CouchDbAbstractTest.PROVIDER)
                                                   .build());
    }

    @AfterEach
    public void after() {
        db.deleteDb();
    }

    @Test
    public void shouldSaveAndDelete() {
        User userJohn = new User("John", 25);
        
        for (User u : db.saveOrUpdate(userJohn, new User("Ivan", 22), new User("Mary", 18))) {
            Assertions.assertTrue(u.isOk());
        }

        Assertions.assertEquals(3, db.getInfo().getDocCount());

        for (YnsBulkResponse res : db.delete(userJohn.getDocIdAndRev())) {
            Assertions.assertTrue(res.isOk());
        }

        Assertions.assertEquals(2, db.getInfo().getDocCount());

        List<User> otherUsers = db.getBuiltInView().<User>createDocQuery().asDocs().stream().filter(d -> d.getClass() == User.class).collect(Collectors.toList());//filter design docs if exists

        for (YnsBulkResponse res : db.delete(otherUsers.stream().map(YnsDocument::getDocIdAndRev).collect(Collectors.toList()))) {
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

        byte[] r = db.getAttachmentAsString(userJohn.getDocId(), "avatar").getBytes();

        Assertions.assertNotNull(r);
    }

    @Test
    public void attachAvatarAsBase64() throws IOException {
        User userJohn = new User("John", 25);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(InputStream in = getClass().getResourceAsStream("/rabbit.gif")) {
            FileCopyUtils.copy(in, out);
        }

        userJohn.addAttachment("avatar", new YnsDocumentAttachment("image/gif", Base64.getEncoder().encodeToString(out.toByteArray())));

        db.saveOrUpdate(userJohn);

        byte[] r = db.getAttachmentAsString(userJohn.getDocId(), "avatar").getBytes();
        
        Assertions.assertNotNull(r);
    }
}
