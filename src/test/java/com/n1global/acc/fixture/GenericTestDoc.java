package com.n1global.acc.fixture;

import com.n1global.acc.json.CouchDbDocument;

public class GenericTestDoc<T> extends CouchDbDocument {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
