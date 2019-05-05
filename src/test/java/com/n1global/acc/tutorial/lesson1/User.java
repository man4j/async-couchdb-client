package com.n1global.acc.tutorial.lesson1;

import com.equiron.acc.json.CouchDbDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User extends CouchDbDocument {
    private String name;

    private int age;

    @JsonCreator
    public User(@JsonProperty("name") String name, @JsonProperty("age") int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
