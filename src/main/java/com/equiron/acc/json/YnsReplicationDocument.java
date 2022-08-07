package com.equiron.acc.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
public class YnsReplicationDocument extends YnsDocument {
    private String source;
    
    private String target;
    
    private Map<String, Object> selector;
    
    private boolean continuous = true;
    
    @JsonProperty("create_target")
    private boolean createTarget = false;

    public YnsReplicationDocument(String docId, String source, String target) {
        setDocId(docId);
        this.source = source;
        this.target = target;
    }
    
    public YnsReplicationDocument(String docId, String source, String target, boolean continuous) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.continuous = continuous;
    }
    
    public YnsReplicationDocument(String docId, String source, String target, Map<String, Object> selector) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.selector = selector;
    }
    
    public YnsReplicationDocument(String docId, String source, String target, Map<String, Object> selector, boolean createTarget) {
        setDocId(docId);
        this.source = source;
        this.target = target;
        this.selector = selector;
        this.createTarget = createTarget;
    }
}
