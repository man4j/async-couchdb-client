package com.n1global.acc.json.taskinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbViewCompactionTaskInfo extends CouchDbTaskInfo {
    private String database;

    @JsonProperty("design_document")
    private String designDocument;

    public String getDatabase() {
        return database;
    }

    public String getDesignDocument() {
        return designDocument;
    }

    @Override
    public String toString() {
        return "CouchDbViewCompactionTaskInfo [database=" + database + ", designDocument=" + designDocument
                + ", getProgress()=" + getProgress() + ", getStartedOn()=" + getStartedOn() + ", getUpdatedOn()="
                + getUpdatedOn() + "]";
    }
}
