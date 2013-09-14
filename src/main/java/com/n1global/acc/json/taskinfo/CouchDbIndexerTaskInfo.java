package com.n1global.acc.json.taskinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbIndexerTaskInfo extends CouchDbTaskInfo {
    @JsonProperty("changes_done")
    private int changesDone;

    private String database;

    @JsonProperty("design_document")
    private String designDocument;

    @JsonProperty("total_changes")
    private int totalChanges;

    public int getChangesDone() {
        return changesDone;
    }

    public String getDatabase() {
        return database;
    }

    public String getDesignDocument() {
        return designDocument;
    }

    public int getTotalChanges() {
        return totalChanges;
    }

    @Override
    public String toString() {
        return "CouchDbIndexerTaskInfo [changesDone=" + changesDone + ", database=" + database + ", designDocument="
                + designDocument + ", totalChanges=" + totalChanges + ", getProgress()=" + getProgress()
                + ", getStartedOn()=" + getStartedOn() + ", getUpdatedOn()=" + getUpdatedOn() + "]";
    }
}
