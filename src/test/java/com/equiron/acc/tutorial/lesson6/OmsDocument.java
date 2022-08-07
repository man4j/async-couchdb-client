package com.equiron.acc.tutorial.lesson6;

import com.equiron.acc.json.YnsDocument;

public class OmsDocument extends YnsDocument {
    private String omsId;
    
    private OmsDocumentStatus status = OmsDocumentStatus.CREATED;

    public OmsDocument(String docId, String omsId) {
        super(docId);
        this.omsId = omsId;
    }
    
    public OmsDocument() {
        //empty
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
