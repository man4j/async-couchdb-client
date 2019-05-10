package com.equiron.acc.fixture;

import com.equiron.acc.json.CouchDbDocument;

public class GenericTestDoc<T> extends CouchDbDocument {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
