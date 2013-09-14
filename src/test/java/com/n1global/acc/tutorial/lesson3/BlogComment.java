package com.n1global.acc.tutorial.lesson3;

public class BlogComment extends BlogDocument {
    private String body;

    private String blogPostId;

    private String authorId;

    public BlogComment() {
        /* empty */
    }

    public BlogComment(String body, String blogPostId, String authorId) {
        this.body = body;
        this.blogPostId = blogPostId;
        this.authorId = authorId;
    }

    public String getBody() {
        return body;
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public String getAuthorId() {
        return authorId;
    }
}
