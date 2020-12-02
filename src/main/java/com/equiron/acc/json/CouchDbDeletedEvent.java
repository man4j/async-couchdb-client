package com.equiron.acc.json;

import com.equiron.acc.CouchDbDocIdAndRev;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbDeletedEvent {
    @JsonProperty("id")
    private String docId;

    private String seq;

    private boolean deleted;
    
    private CouchDbDocIdAndRev doc;

    public String getDocId() {
        return docId;
    }

    public String getSeq() {
        return seq;
    }

    public boolean isDeleted() {
        return deleted;
    }
    
    public CouchDbDocIdAndRev getDoc() {
        return doc;
    }

    @Override
    public String toString() {
        return docId + "/" + seq;
    }
}
