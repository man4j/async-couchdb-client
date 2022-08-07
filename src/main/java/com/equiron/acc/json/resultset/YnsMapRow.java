package com.equiron.acc.json.resultset;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class YnsMapRow<K, V> extends YnsAbstractRow<K, V> {
    @JsonProperty("id")
    @Getter
    private String docId;

    @Getter
    private String error = "";

    /**
     * May occur when we works with built-in views with "keys" param.
     *
     * @return true if row not found.
     */
    boolean isNotFound() {
        return error.equals("not_found");
    }
}
