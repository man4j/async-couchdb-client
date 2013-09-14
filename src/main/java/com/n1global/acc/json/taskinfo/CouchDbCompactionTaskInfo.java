package com.n1global.acc.json.taskinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbCompactionTaskInfo extends CouchDbTaskInfo {
    @JsonProperty("changes_done")
    private int changesDone;

    private String database;

    @JsonProperty("total_changes")
    private int totalChanges;

    public int getChangesDone() {
        return changesDone;
    }

    public String getDatabase() {
        return database;
    }

    public int getTotalChanges() {
        return totalChanges;
    }

    @Override
    public String toString() {
        return "CouchDbCompactionTaskInfo [changesDone=" + changesDone + ", database=" + database + ", totalChanges="
                + totalChanges + ", getProgress()=" + getProgress() + ", getStartedOn()=" + getStartedOn()
                + ", getUpdatedOn()=" + getUpdatedOn() + "]";
    }
}
