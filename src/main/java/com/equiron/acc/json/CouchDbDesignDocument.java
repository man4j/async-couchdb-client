package com.equiron.acc.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_DEFAULT)
public final class CouchDbDesignDocument extends CouchDbDocument {
    private Map<String, CouchDbMapReduceFunction> views = new LinkedHashMap<>();

    @JsonProperty("validate_doc_update")
    private String validateDocUpdate;

    @JsonInclude(Include.NON_NULL)
    private String language = "javascript";

    public CouchDbDesignDocument(String docId) {
        setDocId(docId);
    }

    public CouchDbDesignDocument(String docId, String language) {
        setDocId(docId);

        this.language = language;
    }

    public CouchDbDesignDocument() {
        /* empty */
    }

    public Map<String, CouchDbMapReduceFunction> getViews() {
        return views;
    }

    public void setValidateDocUpdate(String validateDocUpdate) {
        this.validateDocUpdate = validateDocUpdate;
    }
    
    public String getValidateDocUpdate() {
        return validateDocUpdate;
    }

    public void addView(String name, CouchDbMapReduceFunction function) {
        views.put(name, function);
    }

    public void addView(String name, String map, String reduce) {
        views.put(name, new CouchDbMapReduceFunction(map, reduce));
    }

    public void addView(String name, String map) {
        views.put(name, new CouchDbMapReduceFunction(map));
    }

    public CouchDbMapReduceFunction getView(String name) {
        return views.get(name);
    }

    public CouchDbMapReduceFunction deleteView(String name) {
        return views.remove(name);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (this == obj) return true;

        CouchDbDesignDocument other = (CouchDbDesignDocument)obj;

        if (getDocId().equals(other.getDocId()) && views.equals(other.views) && Objects.equals(validateDocUpdate, other.validateDocUpdate)) return true;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDocId(), views, validateDocUpdate);
    }
}
