package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsRevisions {
    private String status;

    private String rev;
}
