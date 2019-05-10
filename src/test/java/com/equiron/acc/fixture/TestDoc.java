package com.equiron.acc.fixture;

import com.equiron.acc.json.CouchDbDocument;

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
