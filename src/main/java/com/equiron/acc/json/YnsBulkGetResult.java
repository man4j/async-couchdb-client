package com.equiron.acc.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsBulkGetResult<T> {
    @JsonProperty("id")
    private String docId;

    private List<YnsBulkGetResultItem<T>> docs = new ArrayList<>();
}
