package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbBooleanResponse {
    private boolean ok;

    public boolean isOk() {
        return ok;
    }
}
