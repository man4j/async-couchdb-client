package com.equiron.acc.tutorial.lesson3;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Author extends BlogDocument {
    private String name;

    private List<String> blogPostsIds = new ArrayList<>();

    @JsonCreator
    public Author(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getBlogPostsIds() {
        return blogPostsIds;
    }
}
