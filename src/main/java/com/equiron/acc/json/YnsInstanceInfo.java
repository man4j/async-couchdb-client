package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsInstanceInfo {
    private String version;
    
    private String uuid;
}
