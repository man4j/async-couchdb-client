package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YnsBooleanResponse {
    @Getter
    private boolean ok;
}
