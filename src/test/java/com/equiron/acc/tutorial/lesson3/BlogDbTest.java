package com.equiron.acc.tutorial.lesson3;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.equiron.acc.YnsAbstractTest;
import com.equiron.acc.YnsDbConfig;
import com.equiron.acc.json.YnsDocument;
import com.equiron.acc.json.resultset.YnsReduceRow;

public class BlogDbTest {
    private BlogDb db;

    @BeforeEach
    public void before() {
        db = new BlogDb(new YnsDbConfig.Builder().setHost(System.getProperty("HOST"))
                                                 .setPort(Integer.parseInt(System.getProperty("PORT")))
                                                 .setUser(System.getProperty("USER"))
                                                 .setPassword(System.getProperty("PASSWORD"))
                                                 .setHttpClientProviderType(YnsAbstractTest.PROVIDER)
                                                 .build());
    }
    
    @AfterEach
    public void after() {
        db.deleteDb();
    }
    
    @Test
    public void shouldGetPopularPost() {
        Author author1 = new Author("John");
        Author author2 = new Author("Sally");

        db.saveOrUpdate(List.of(author1, author2));

        BlogPost blogPost1 = new BlogPost("First post", "This is first post!", author1.getDocId());
        BlogPost blogPost2 = new BlogPost("Second post", "This is second post!", author2.getDocId());

        db.saveOrUpdate(List.of(blogPost1, blogPost2));

        author1.getBlogPostsIds().add(blogPost1.getDocId());//because author1 is author of blogPost1
        author2.getBlogPostsIds().add(blogPost2.getDocId());//because author2 is author of blogPost2

        db.saveOrUpdate(List.of(author1, author2));

        db.saveOrUpdate(List.of(new BlogComment("Hey John! This is cool post!", blogPost1.getDocId(), author2.getDocId()),
                                new BlogComment("Thanks, Sally!", blogPost1.getDocId(), author1.getDocId()),
                                new BlogComment("Hey Sally! This is cool post!", blogPost2.getDocId(), author1.getDocId())));
        
        List<String> postIds = db.getPostsByDate().createQuery()
                                                  .startKey(LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
                                                  .endKey(System.currentTimeMillis())
                                                  .asIds();
        
        String popularPostId = db.getCommentsCountByPost()
                                 .createReduceQuery()
                                 .byKeys(postIds)
                                 .group()
                                 .asRows()
                                 .stream()
                                 .sorted(Comparator.comparingLong(YnsReduceRow<String, Long>::getValue).reversed())
                                 .findFirst()
                                 .orElse(null)
                                 .getKey();
        
        Assertions.assertEquals(blogPost1.getDocId(), popularPostId);
    }
    
    @Test
    public void shouldFetchAllWithOneQuery() {
        Author author1 = new Author("John");
        Author author2 = new Author("Sally");

        db.saveOrUpdate(List.of(author1, author2));

        BlogPost blogPost1 = new BlogPost("First post", "This is first post!", author1.getDocId());
        BlogPost blogPost2 = new BlogPost("Second post", "This is second post!", author2.getDocId());

        db.saveOrUpdate(List.of(blogPost1, blogPost2));

        author1.getBlogPostsIds().add(blogPost1.getDocId());//because author1 is author of blogPost1
        author2.getBlogPostsIds().add(blogPost2.getDocId());//because author2 is author of blogPost2

        db.saveOrUpdate(List.of(author1, author2));

        db.saveOrUpdate(List.of(new BlogComment("Hey John! This is cool post!", blogPost1.getDocId(), author2.getDocId()),
                                new BlogComment("Thanks, Sally!", blogPost1.getDocId(), author1.getDocId()),
                                new BlogComment("Hey Sally! This is cool post!", blogPost2.getDocId(), author1.getDocId()),
                                new BlogComment("Thanks, John!", blogPost2.getDocId(), author2.getDocId())));

        author1.getBlogPostsIds().add(blogPost2.getDocId());//because author1 commented blogPost2
        author2.getBlogPostsIds().add(blogPost1.getDocId());//because author2 commented blogPost1

        db.saveOrUpdate(List.of(author1, author2));

        // Fetch
        List<BlogDocument> blogPostDocuments = db.get(db.getJoinedView().createQuery().byKey(blogPost1.getDocId()).asIds(), BlogDocument.class);

        List<BlogDocument> comments = blogPostDocuments.stream().filter(d -> d.getClass() == BlogComment.class).collect(Collectors.toList());

        BlogDocument post = blogPostDocuments.stream().filter(d -> d.getClass() == BlogPost.class).findFirst().get();

        Map<String, Author> authors = new HashMap<>();

        for (YnsDocument author : blogPostDocuments.stream().filter(d -> d.getClass() == Author.class).collect(Collectors.toList())) {
            authors.put(author.getDocId(), (Author)author);
        }
        
        Assertions.assertEquals("John", authors.get(((BlogPost)post).getOwnerId()).getName());//John is blog post author
        Assertions.assertEquals("Sally", authors.get(((BlogComment)comments.get(0)).getOwnerId()).getName());//Sally is author of the first comment
        Assertions.assertEquals("John", authors.get(((BlogComment)comments.get(1)).getOwnerId()).getName());//John is author of the second comment

        Assertions.assertEquals(2, comments.size());
        Assertions.assertEquals(2, authors.size());
        Assertions.assertEquals(5, blogPostDocuments.size());
    }
}