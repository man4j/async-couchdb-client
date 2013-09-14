package com.n1global.acc.tutorial.lesson7;

public class Topic extends ForumContent {
    private String text;

    public Topic() {
        /* empty */
    }

    public Topic(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
