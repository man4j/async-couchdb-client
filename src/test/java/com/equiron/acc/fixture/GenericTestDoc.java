package com.equiron.acc.fixture;

import com.equiron.acc.json.YnsDocument;

public class GenericTestDoc<T> extends YnsDocument {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
