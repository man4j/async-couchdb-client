package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsDesignInfo {
    /**
     * Name of the design document without the _design prefix.
     */
    private String name;

    /**
     * Contains information on the views.
     */
    @JsonProperty("view_index")
    private YnsViewInfo viewInfo;
}
