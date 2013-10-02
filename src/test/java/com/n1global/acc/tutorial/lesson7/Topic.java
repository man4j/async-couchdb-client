package com.n1global.acc.tutorial.lesson7;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Topic extends ForumContent {
    private String text;

    @JsonCreator
    public Topic(@JsonProperty("text") String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
