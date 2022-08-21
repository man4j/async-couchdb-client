package com.equiron.acc.fixture;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = GenericTestDoc.class,           name = "GenericTestDoc"),
               @JsonSubTypes.Type(value = GenericTestDocDescendant.class, name = "GenericTestDocDescendant")})
public class GenericTestDoc<T> extends YnsDocument {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
