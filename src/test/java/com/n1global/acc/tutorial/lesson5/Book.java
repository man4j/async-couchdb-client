package com.n1global.acc.tutorial.lesson5;

import com.n1global.acc.json.CouchDbDocument;

public class Book extends CouchDbDocument {
    private String title;

    private String publisherName;

    public Book() {
        /* empty */
    }

    public Book(String title, String publisherName) {
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
