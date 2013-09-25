package com.n1global.acc.tutorial.lesson3;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.FluentIterable;
import com.n1global.acc.CouchDbConfig;
import com.ning.http.client.AsyncHttpClient;

public class BlogDbTest {
    private BlogDb db;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    @Before
    public void before() {
        db = new BlogDb(new CouchDbConfig.Builder().setUser("root")
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

        List<BlogComment> comments = FluentIterable.from(blogRelatedDocs).filter(BlogComment.class).toList();

        BlogPost post = FluentIterable.from(blogRelatedDocs).filter(BlogPost.class).first().get();

        Map<String, Author> authors = new HashMap<>();

        for (Author author : FluentIterable.from(blogRelatedDocs).filter(Author.class)) {
            authors.put(author.getDocId(), author);
        }

        Assert.assertEquals("John", authors.get(post.getAuthorId()).getName());//John is blog post author
        Assert.assertEquals("Sally", authors.get(comments.get(0).getAuthorId()).getName());//Sally is author of the first comment
        Assert.assertEquals("John", authors.get(comments.get(1).getAuthorId()).getName());//John is author of the second comment

        Assert.assertEquals(2, comments.size());
        Assert.assertEquals(2, authors.size());
        Assert.assertEquals(5, blogRelatedDocs.size());
    }
}