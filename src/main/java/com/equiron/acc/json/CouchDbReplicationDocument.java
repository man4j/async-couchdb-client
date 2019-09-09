package com.equiron.acc.json;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbReplicationDocument extends CouchDbDocument {
    private String source;
    
    private String target;
    
    private Map<String, Object> selector;
    
    private boolean continuous = true;
    
    @JsonProperty("create_target")
    private boolean createTarget = false;

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
    
    public boolean isCreateTarget() {
        return createTarget;
    }

    public void setCreateTarget(boolean createTarget) {
        this.createTarget = createTarget;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (this == obj) return true;

        CouchDbReplicationDocument other = (CouchDbReplicationDocument)obj;

        if (getDocId().equals(other.getDocId()) && source.equals(other.source) && target.equals(other.target) && Objects.equals(selector, other.selector) && (createTarget == other.createTarget)) return true;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDocId(), source, target, selector, createTarget);
    }
}
