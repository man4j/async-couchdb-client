package com.equiron.acc.tutorial.lesson2;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class City extends YnsDocument {
    @JsonCreator
    public City(@JsonProperty("name") String name) {
        setDocId(name);
    }

    public String getName() {
        return getDocId();
    }
}