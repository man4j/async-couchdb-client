package com.equiron.yns.tutorial.lesson3;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class Author extends BlogDocument {
    private String name;

    private List<String> blogPostsIds = new ArrayList<>();

    @JsonCreator
    public Author(@JsonProperty("name") String name) {
        this.name = name;
    }
}
