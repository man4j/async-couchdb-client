package com.equiron.acc.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbDocIdAndRev;
import com.equiron.acc.util.HasId;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonAutoDetect(fieldVisibility=Visibility.ANY,
                creatorVisibility=Visibility.NONE,
                getterVisibility=Visibility.NONE,
                isGetterVisibility=Visibility.NONE,
                setterVisibility=Visibility.NONE)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use=Id.CLASS)
public class CouchDbDocument implements HasId<String> {
    @JsonProperty("_id")
    private String docId;

    @JsonProperty("_rev")
    private String rev;

    @JsonProperty("_attachments")
    @JsonInclude(Include.NON_EMPTY)
    private Map<String, CouchDbDocumentAttachment> attachments = new HashMap<>();

    @JsonProperty("_revs_info")
    @JsonInclude(Include.NON_EMPTY)
    private List<CouchDbRevisions> revisions = new ArrayList<>();

    @JsonProperty("_deleted")
    private Boolean deleted;

    @JsonIgnore
    boolean inConflict;

    @JsonIgnore
    boolean forbidden;

    @JsonIgnore
    String conflictReason;
    
    @JsonIgnore
    String bulkError;

    @JsonIgnore
    CouchDb currentDb;

    public CouchDbDocument() {
        /* empty */
    }

    public CouchDbDocument(String docId) {
        setDocId(docId);
    }

    @Override
    public String getUniqueId() {
        return docId;
    }

    public CouchDbDocIdAndRev getDocIdAndRev() {
        return new CouchDbDocIdAndRev(getDocId(), getRev());
    }

    /**
     * @return the unique identifier of the document.
     */
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    /**
     * @return the current MVCC-token/revision of this document.
     */
    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    /**
     * If the document has attachments, _attachments holds a (meta-)data structure.
     */
    public Map<String, CouchDbDocumentAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, CouchDbDocumentAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(String name, CouchDbDocumentAttachment attachment) {
        attachments.put(name, attachment);
    }

    public CouchDbDocumentAttachment getAttachment(String name) {
        return attachments.get(name);
    }

    public CouchDbDocumentAttachment deleteAttachment(String name) {
        return attachments.remove(name);
    }

    /**
     * Indicates that this document has been deleted and previous revisions will be removed on next compaction run.
     */
    public boolean isDeleted() {
        return deleted == null ? false : deleted;
    }

    public void setDeleted() {
        this.deleted = true;
    }

    public boolean isInConflict() {
        return inConflict;
    }

    public boolean isForbidden() {
        return forbidden;
    }
    
    public boolean isOk() {
        return !isInConflict() && !isForbidden();
    }

    public String getConflictReason() {
        return conflictReason;
    }
    
    public String getBulkError() {
        return bulkError;
    }

    /**
     * @return a list of revisions of the document, and their availability.
     */
    public List<CouchDbRevisions> getRevisions() {
        return revisions;
    }

    public CouchDb getCurrentDb() {
        return currentDb;
    }

    @Override
    public String toString() {
        return docId + " / " + rev;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        CouchDbDocument other = (CouchDbDocument) obj;

        return getDocId().equals(other.getDocId());
    }

    @Override
    public int hashCode() {
        return getDocId().hashCode();
    }
}