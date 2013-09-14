package com.n1global.acc.json.resultset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CouchDbAbstractRow<K, V> {
    private K key;

    private V value;

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
