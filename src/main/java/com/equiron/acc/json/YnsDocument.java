package com.equiron.acc.json;

import java.util.HashMap;
import java.util.Map;

import com.equiron.acc.YnsDocIdAndRev;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonAutoDetect(fieldVisibility=Visibility.ANY,
                creatorVisibility=Visibility.NONE,
                getterVisibility=Visibility.NONE,
                isGetterVisibility=Visibility.NONE,
                setterVisibility=Visibility.NONE)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class YnsDocument {
    /**
     * @return the unique identifier of the document.
     */
    @JsonProperty("_id")
    @Getter
    @Setter
    @EqualsAndHashCode.Include
    private String docId;

    /**
     * @return the current MVCC-token/revision of this document.
     */
    @JsonProperty("_rev")
    @Getter
    @Setter
    @EqualsAndHashCode.Include
    private String rev;

    @JsonProperty("_deleted")
    @Setter
    private Boolean deleted;
    
    /**
     * If the document has attachments, _attachments holds a (meta-)data structure.
     */
    @JsonProperty("_attachments")
    @JsonInclude(Include.NON_EMPTY)
    @Getter
    @Setter
    private Map<String, YnsDocumentAttachment> attachments = new HashMap<>();

    public YnsDocument(String docId) {
        setDocId(docId);
    }
    
    public YnsDocIdAndRev getDocIdAndRev() {
        return new YnsDocIdAndRev(getDocId(), getRev());
    }

    /**
     * Indicates that this document has been deleted and previous revisions will be removed on next compaction run.
     */
    public boolean isDeleted() {
        return deleted == null ? false : deleted;
    }

    @Override
    public String toString() {
        return docId + " / " + rev;
    }
}