package com.equiron.acc.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsBulkGetResponse {
    private List<YnsBulkGetResult> results = new ArrayList<>();
}
