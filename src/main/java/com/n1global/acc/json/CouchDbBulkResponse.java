package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.n1global.acc.CouchDbDocIdAndRev;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbBulkResponse {
    @JsonProperty("id")
    private String docId;

    private String rev;

    private String error = "";

    @JsonProperty("reason")
    private String conflictReason = "";

    public String getDocId() {
        return docId;
    }

    public String getRev() {
        return rev;
    }

    public boolean isInConflict() {
        return error.contains("conflict");
    }

    public boolean isForbidden() {
        return error.contains("forbidden");
    }

    public String getError() {
        return error;
    }

    public String getConflictReason() {
        return conflictReason;
    }

    public CouchDbDocIdAndRev getDocIdAndRev() {
        return new CouchDbDocIdAndRev(docId, rev);
    }
}
