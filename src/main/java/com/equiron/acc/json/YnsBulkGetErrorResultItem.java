package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsBulkGetErrorResultItem implements YnsBulkGetResultItem {
    @JsonProperty("id")
    private String docId;

    private String rev;

    private String error = "";

    private String reason = "";
}
