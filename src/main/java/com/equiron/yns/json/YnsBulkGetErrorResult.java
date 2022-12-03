package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class YnsBulkGetErrorResult {
    @JsonProperty("id")
    private String docId;

    private String rev;

    private String error = "";

    private String reason = "";
}
