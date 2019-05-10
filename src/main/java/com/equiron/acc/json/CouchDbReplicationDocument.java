package com.equiron.acc.json;

public class CouchDbReplicationDocument extends CouchDbDocument {
    private String source;
    
    private String target;
    
    private String selector;
    
    private boolean continuous = true;

    public CouchDbReplicationDocument() {
        /* empty */
    }

    public CouchDbReplicationDocument(String docId, String source, String target) {
        setDocId(docId);
        this.source = source;
        this.target = target;
    }
    
    public CouchDbReplicationDocument(String docId, String source, String target, boolean continuous) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.continuous = continuous;
    }
    
    public CouchDbReplicationDocument(String docId, String source, String target, String selector) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.selector = selector;
    }
    
    public CouchDbReplicationDocument(String docId, String source, String target, String selector, boolean continuous) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.selector = selector;
        this.continuous = continuous;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
    
    public String getSelector() {
        return selector;
    }
}
