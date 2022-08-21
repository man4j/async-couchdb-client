package com.equiron.acc.fixture;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = TestDoc.class,           name = "TestDoc"),
               @JsonSubTypes.Type(value = TestDocDescendant.class, name = "TestDocDescendant")})
public class TestDoc extends YnsDocument {
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
