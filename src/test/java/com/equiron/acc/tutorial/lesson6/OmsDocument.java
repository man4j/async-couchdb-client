package com.equiron.acc.tutorial.lesson6;

import com.equiron.acc.json.YnsDocument;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class OmsDocument extends YnsDocument {
    @Getter
    private String omsId;
    
    @Getter
    @Setter
    private OmsDocumentStatus status = OmsDocumentStatus.CREATED;

    public OmsDocument(String docId, String omsId) {
        super(docId);
        this.omsId = omsId;
    }
}
