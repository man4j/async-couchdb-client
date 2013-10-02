package com.n1global.acc.tutorial.lesson3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BlogPost extends BlogDocument {
    private String title;

    private String body;

    private String authorId;

    @JsonCreator
    public BlogPost(@JsonProperty("title") String title, @JsonProperty("body") String body, @JsonProperty("authorId") String authorId) {
        this.title = title;
        this.body = body;
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthorId() {
        return authorId;
    }
}
