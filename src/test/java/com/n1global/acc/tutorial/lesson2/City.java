package com.n1global.acc.tutorial.lesson2;

import com.n1global.acc.json.CouchDbDocument;

public class City extends CouchDbDocument {
    public City(String name) {
        setDocId(name);
    }

    public String getName() {
        return getDocId();
    }

    public City() {}
}