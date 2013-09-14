package com.n1global.acc.fixture;

import com.n1global.acc.json.CouchDbDocument;

public class TestDoc extends CouchDbDocument {
    private String name;

    public TestDoc(String name) {
        this.name = name;
    }

    public TestDoc() {
        //empty
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
