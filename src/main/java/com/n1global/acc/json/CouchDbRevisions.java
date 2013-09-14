package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbRevisions {
    private String status;

    private String rev;

    public String getStatus() {
        return status;
    }

    public String getRev() {
        return rev;
    }
}
