package com.equiron.acc.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(Include.NON_DEFAULT)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class YnsDesignDocument extends YnsDocument {
    @Getter
    private Map<String, YnsMapReduceFunction> views = new LinkedHashMap<>();

    @JsonProperty("validate_doc_update")
    @Getter
    @Setter
    private String validateDocUpdate;

    @JsonInclude(Include.NON_NULL)
    @Getter
    @Setter
    private String language = "javascript";

    public YnsDesignDocument(String docId) {
        setDocId(docId);
    }

    public YnsDesignDocument(String docId, String language) {
        setDocId(docId);

        this.language = language;
    }

    public void addView(String name, YnsMapReduceFunction function) {
        views.put(name, function);
    }

    public void addView(String name, String map, String reduce) {
        views.put(name, new YnsMapReduceFunction(map, reduce));
    }

    public void addView(String name, String map) {
        views.put(name, new YnsMapReduceFunction(map));
    }

    public YnsMapReduceFunction getView(String name) {
        return views.get(name);
    }

    public YnsMapReduceFunction deleteView(String name) {
        return views.remove(name);
    }
}
