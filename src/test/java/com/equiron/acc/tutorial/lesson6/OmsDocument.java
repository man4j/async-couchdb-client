package com.equiron.acc.tutorial.lesson6;

import com.equiron.acc.json.CouchDbDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmsDocument extends CouchDbDocument {
    private String omsId;
    
    private OmsDocumentStatus status = OmsDocumentStatus.CREATED;

    @JsonCreator
    public OmsDocument(@JsonProperty("omsId") String omsId) {
        this.omsId = omsId;
    }

    public String getOmsId() {
        return omsId;
    }

    public OmsDocumentStatus getStatus() {
        return status;
    }
    
    public void setStatus(OmsDocumentStatus status) {
        this.status = status;
    }
}
