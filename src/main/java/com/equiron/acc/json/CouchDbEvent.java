package com.equiron.acc.json;

import com.equiron.acc.CouchDbDocIdAndRev;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbEvent<D extends CouchDbDocument> {
    @JsonProperty("id")
    private String docId;

    private String seq;

    private boolean deleted;
    
    @JsonIgnore
    private CouchDbDocIdAndRev docIdAndRev;

    private D doc;
    
    public CouchDbEvent() {
        //empty
    }

    public CouchDbEvent(String docId, String seq, boolean deleted, CouchDbDocIdAndRev docIdAndRev) {
        this.docId = docId;
        this.seq = seq;
        this.deleted = deleted;
        this.docIdAndRev = docIdAndRev;
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
    
    public CouchDbDocIdAndRev getDocIdAndRev() {
        return docIdAndRev == null ? doc.getDocIdAndRev() : docIdAndRev;
    }

    @Override
    public String toString() {
        return docId + "/" + seq;
    }
}
