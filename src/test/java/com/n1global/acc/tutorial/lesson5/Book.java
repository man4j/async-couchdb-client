package com.n1global.acc.tutorial.lesson5;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.n1global.acc.json.CouchDbDocument;

public class Book extends CouchDbDocument {
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
