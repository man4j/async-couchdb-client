package com.n1global.acc.tutorial.lesson1;

import com.n1global.acc.json.CouchDbDocument;

public class User extends CouchDbDocument {
    private String name;

    private int age;

    public User() {
        //empty
    }

    public User(String name, int age) {
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
