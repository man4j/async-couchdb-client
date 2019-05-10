package com.equiron.acc.tutorial.lesson3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BlogComment extends BlogDocument {
    private String body;

    private String blogPostId;

    private String authorId;

    @JsonCreator
    public BlogComment(@JsonProperty("body") String body, @JsonProperty("blogPostId") String blogPostId, @JsonProperty("authorId") String authorId) {
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
