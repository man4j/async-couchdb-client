package com.n1global.acc.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_DEFAULT)
public final class CouchDbDesignDocument extends CouchDbDocument {
    private Map<String, CouchDbMapReduceFunction> views = new LinkedHashMap<>();

    private Map<String, String> filters = new LinkedHashMap<>();
    
    @JsonProperty("validate_doc_update")
    private String validateDocUpdate;

    @JsonProperty("updates")
    private Map<String, String> updatesHandlers = new LinkedHashMap<>();

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

    public Map<String, String> getFilters() {
        return filters;
    }
    
    public void setValidateDocUpdate(String validateDocUpdate) {
        this.validateDocUpdate = validateDocUpdate;
    }

    public Map<String, String> getUpdatesHandlers() {
        return updatesHandlers;
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

    public void addFilter(String name, String fun) {
        filters.put(name, fun);
    }

    public void addUpdateHandler(String name, String fun) {
        updatesHandlers.put(name, fun);
    }

    public CouchDbMapReduceFunction getView(String name) {
        return views.get(name);
    }

    public String getFilter(String name) {
        return filters.get(name);
    }

    public String getUpdateHandler(String name) {
        return updatesHandlers.get(name);
    }

    public CouchDbMapReduceFunction deleteView(String name) {
        return views.remove(name);
    }

    public String deleteFilter(String name) {
        return filters.remove(name);
    }

    public String deleteUpdateHandler(String name) {
        return updatesHandlers.remove(name);
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

        if (views.equals(other.views)
            && filters.equals(other.filters)
            && updatesHandlers.equals(other.updatesHandlers)) return true;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(views, filters, updatesHandlers);
    }
}
