package com.equiron.acc.json.resultset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class YnsAbstractRow<K, V> {
    @Getter
    private K key;

    @Getter
    private V value;
}
