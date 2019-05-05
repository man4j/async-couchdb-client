package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbDesignInfo {
    private String name;

    @JsonProperty("view_index")
    private CouchDbViewInfo viewInfo;

    /**
     * Name of the design document without the _design prefix.
     */
    public String getName() {
        return name;
    }

    /**
     * Contains information on the views.
     */
    public CouchDbViewInfo getViewInfo() {
        return viewInfo;
    }
}
