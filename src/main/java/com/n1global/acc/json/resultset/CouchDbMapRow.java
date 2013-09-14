package com.n1global.acc.json.resultset;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbMapRow<K, V> extends CouchDbAbstractRow<K, V> {
    @JsonProperty("id")
    private String docId;

    private String error = "";

    public String getDocId() {
        return docId;
    }

    public String getError() {
        return error;
    }

    /**
     * May occur when we works with built-in views with "keys" param.
     *
     * @return true if row not found.
     */
    boolean isNotFound() {
        return error.equals("not_found");
    }
}
