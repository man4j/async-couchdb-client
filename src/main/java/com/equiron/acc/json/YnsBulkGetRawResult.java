package com.equiron.acc.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsBulkGetRawResult {
    @JsonProperty("id")
    private String docId;

    private List<Map<String, Map<String, Object>>> docs = new ArrayList<>();
}
