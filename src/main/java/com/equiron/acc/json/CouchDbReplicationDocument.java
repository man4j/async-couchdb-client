package com.equiron.acc.json;

import java.util.Map;

public class CouchDbReplicationDocument extends CouchDbDocument {
    private String source;
    
    private String target;
    
    private Map<String, Object> selector;
    
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
    
    public CouchDbReplicationDocument(String docId, String source, String target, Map<String, Object> selector) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.selector = selector;
    }
    
    public CouchDbReplicationDocument(String docId, String source, String target, Map<String, Object> selector, boolean continuous) {
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
    
    public Map<String, Object> getSelector() {
        return selector;
    }
}
