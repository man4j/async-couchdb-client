package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsEvent {
    @JsonProperty("id")
    private String docId;

    private String seq;

    private boolean deleted;
    
    private YnsDocRev2[] changes;
    
    @JsonIgnore
    private boolean isNew;
    
    @JsonCreator
    public YnsEvent(@JsonProperty("id") String docId, 
                    @JsonProperty("seq") String seq, 
                    @JsonProperty("deleted") boolean deleted,
                    @JsonProperty("changes") YnsDocRev2[] changes) {
        this.docId = docId;
        this.seq = seq;
        this.deleted = deleted;
        this.changes = changes;
        this.isNew = changes[0].getRev().startsWith("1-");
    }

    @Override
    public String toString() {
        return docId + "/" + seq;
    }
}
