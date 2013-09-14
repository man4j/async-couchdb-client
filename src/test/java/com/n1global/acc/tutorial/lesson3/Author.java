package com.n1global.acc.tutorial.lesson3;

import java.util.ArrayList;
import java.util.List;

public class Author extends BlogDocument {
    private String name;

    private List<String> blogPostsIds = new ArrayList<>();

    public Author() {
        /* empty */
    }

    public Author(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getBlogPostsIds() {
        return blogPostsIds;
    }
}
