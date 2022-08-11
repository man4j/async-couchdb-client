package com.equiron.acc.json;

import com.equiron.acc.YnsDocIdAndRev;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsBulkResponse {
    @JsonProperty("id")
    private String docId;

    private String rev;

    private String error = "";

    @JsonProperty("reason")
    private String conflictReason = "";

    public boolean isInConflict() {
        return error.contains("conflict");
    }

    public boolean isForbidden() {
        return error.contains("forbidden");
    }
    
    public boolean isOk() {
        return !isInConflict() && !isForbidden() && (error == null || error.isBlank());
    }
    
    public boolean isUnknownError() {
        return !isInConflict() && !isForbidden() && (error != null && !error.isBlank());
    }

    public YnsDocIdAndRev getDocIdAndRev() {
        return new YnsDocIdAndRev(docId, rev);
    }
}
