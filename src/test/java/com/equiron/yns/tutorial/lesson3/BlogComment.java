package com.equiron.yns.tutorial.lesson3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class BlogComment extends BlogContent {
    private String blogPostId;

    @JsonCreator
    public BlogComment(@JsonProperty("content") String content, 
                       @JsonProperty("blogPostId") String blogPostId,
                       @JsonProperty("ownerId") String ownerId) {
        super(content, ownerId);
        this.blogPostId = blogPostId;
    }
}
