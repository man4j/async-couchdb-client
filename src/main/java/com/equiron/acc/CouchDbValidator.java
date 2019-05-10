package com.equiron.acc;

public class CouchDbValidator {
    private String predicate;

    public CouchDbValidator(String predicate) {
        this.predicate = predicate;
    }

    public String getPredicate() {
        return predicate;
    }
}
