package com.equiron.acc.tutorial.lesson3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogPost extends BlogContent {
    private String title;

    @JsonCreator
    public BlogPost(@JsonProperty("title") String title, 
                    @JsonProperty("content") String content, 
                    @JsonProperty("ownerId") String ownerId) {
        super(content, ownerId);
        this.title = title;
    }
}
