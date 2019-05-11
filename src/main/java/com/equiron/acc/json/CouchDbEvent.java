package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbEvent<D extends CouchDbDocument> {
    @JsonProperty("id")
    private String docId;

    private String seq;

    private boolean deleted;

    private D doc;

    @JsonCreator
    public CouchDbEvent(@JsonProperty("id") String docId, @JsonProperty("seq") String seq, @JsonProperty("deleted") boolean deleted) {
        this.docId = docId;
        this.seq = seq;
        this.deleted = deleted;
    }

    public String getDocId() {
        return docId;
    }

    public String getSeq() {
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
        return docId + "/" + seq;
    }
}
