package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbEvent<D extends CouchDbDocument> {
    @JsonProperty("id")
    private String docId;

    private long seq;

    private boolean deleted;

    private D doc;

    public CouchDbEvent() {
        /* empty */
    }

    public CouchDbEvent(String docId, long seq, boolean deleted) {
        this.docId = docId;
        this.seq = seq;
        this.deleted = deleted;
    }

    public String getDocId() {
        return docId;
    }

    public long getSeq() {
        return seq;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public D getDoc() {
        return doc;
    }

    @Override
    public String toString() {
        return docId;
    }
}
