package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbDocRev {
    private String rev;

    private boolean deleted;

    public String getRev() {
        return rev;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
