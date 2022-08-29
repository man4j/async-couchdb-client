package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsDocRev {
    private String rev;

    private boolean deleted;
}
