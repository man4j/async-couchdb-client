package com.equiron.acc.tutorial.lesson5;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Book extends YnsDocument {
    private String title;

    private String publisherName;

    @JsonCreator
    public Book(@JsonProperty("title") String title, @JsonProperty("publisherName") String publisherName) {
        this.title = title;
        this.publisherName = publisherName;
    }

    public String getTitle() {
        return title;
    }

    public String getPublisherName() {
        return publisherName;
    }
}
