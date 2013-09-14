package com.n1global.acc.json.taskinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({@Type(value = CouchDbIndexerTaskInfo.class, name = "indexer"),
               @Type(value = CouchDbViewCompactionTaskInfo.class, name = "view_compaction"),
               @Type(value = CouchDbReplicationTaskInfo.class, name = "replication"),
               @Type(value = CouchDbCompactionTaskInfo.class, name = "database_compaction")})
@JsonTypeInfo(include=As.PROPERTY, use=Id.NAME, property="type")
public abstract class CouchDbTaskInfo {
    private int progress;

    @JsonProperty("started_on")
    private long startedOn;

    @JsonProperty("updated_on")
    private long updatedOn;

    public int getProgress() {
        return progress;
    }

    public long getStartedOn() {
        return startedOn;
    }

    public long getUpdatedOn() {
        return updatedOn;
    }
}
