package com.n1global.acc.tutorial.lesson2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.n1global.acc.json.CouchDbDocument;

public class City extends CouchDbDocument {
    @JsonCreator
    public City(@JsonProperty("name") String name) {
        setDocId(name);
    }

    public String getName() {
        return getDocId();
    }
}