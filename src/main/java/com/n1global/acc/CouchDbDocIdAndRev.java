package com.n1global.acc;

public class CouchDbDocIdAndRev {
    private String docId;

    private String rev;

    public CouchDbDocIdAndRev(String docId, String rev) {
        this.docId = docId;
        this.rev = rev;
    }

    public String getDocId() {
        return docId;
    }

    public String getRev() {
        return rev;
    }
}
