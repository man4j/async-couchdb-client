package com.n1global.acc.tutorial.lesson3;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.n1global.acc.CouchDbConfig;
import com.n1global.acc.json.CouchDbDocument;
import com.ning.http.client.AsyncHttpClient;

public class BlogDbTest {
    private BlogDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new BlogDb(new CouchDbConfig.Builder().setUser("admin")
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
    public void shouldFetchAllWithOneQuery() {
        Author author1 = new Author("John");
        Author author2 = new Author("Sally");

        db.bulk(author1, author2);

        BlogPost blogPost1 = new BlogPost("First post", "This is first post!", author1.getDocId());
        BlogPost blogPost2 = new BlogPost("Second post", "This is second post!", author2.getDocId());

        db.bulk(blogPost1, blogPost2);

        author1.getBlogPostsIds().add(blogPost1.getDocId());//because author1 is author of blogPost1
        author2.getBlogPostsIds().add(blogPost2.getDocId());//because author2 is author of blogPost2

        db.bulk(author1, author2);

        db.bulk(new BlogComment("Hey John! This is cool post!", blogPost1.getDocId(), author2.getDocId()),
                new BlogComment("Thanks, Sally!", blogPost1.getDocId(), author1.getDocId()),
                new BlogComment("Hey Sally! This is cool post!", blogPost2.getDocId(), author1.getDocId()),
                new BlogComment("Thanks, John!", blogPost2.getDocId(), author2.getDocId()));

        author1.getBlogPostsIds().add(blogPost2.getDocId());//because author1 commented blogPost2
        author2.getBlogPostsIds().add(blogPost1.getDocId());//because author2 commented blogPost1

        db.bulk(author1, author2);

        // Fetch
        List<BlogDocument> blogRelatedDocs = db.getJoinedView().<BlogDocument>createDocQuery().byKey(blogPost1.getDocId()).asDocs();

        Collections.sort(blogRelatedDocs);//sort by timestamps

        List<BlogDocument> comments = blogRelatedDocs.stream().filter(d -> d.getClass() == BlogComment.class).collect(Collectors.toList());

        CouchDbDocument post = blogRelatedDocs.stream().filter(d -> d.getClass() == BlogPost.class).findFirst().get();

        Map<String, Author> authors = new HashMap<>();

        for (CouchDbDocument author : blogRelatedDocs.stream().filter(d -> d.getClass() == Author.class).collect(Collectors.toList())) {
            authors.put(author.getDocId(), (Author)author);
        }

        Assert.assertEquals("John", authors.get(((BlogPost)post).getAuthorId()).getName());//John is blog post author
        Assert.assertEquals("Sally", authors.get(((BlogComment)comments.get(0)).getAuthorId()).getName());//Sally is author of the first comment
        Assert.assertEquals("John", authors.get(((BlogComment)comments.get(1)).getAuthorId()).getName());//John is author of the second comment

        Assert.assertEquals(2, comments.size());
        Assert.assertEquals(2, authors.size());
        Assert.assertEquals(5, blogRelatedDocs.size());
    }
}