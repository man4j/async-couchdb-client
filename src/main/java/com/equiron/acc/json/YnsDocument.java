package com.equiron.acc.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsDocIdAndRev;
import com.equiron.acc.util.HasId;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class YnsDocument implements HasId<String> {
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

    /**
     * If the document has attachments, _attachments holds a (meta-)data structure.
     */
    @JsonProperty("_attachments")
    @JsonInclude(Include.NON_EMPTY)
    @Getter
    @Setter
    private Map<String, YnsDocumentAttachment> attachments = new HashMap<>();

    /**
     * @return a list of revisions of the document, and their availability.
     */
    @JsonProperty("_revs_info")
    @JsonInclude(Include.NON_EMPTY)
    @Getter
    private List<YnsRevisions> revisions = new ArrayList<>();

    @JsonProperty("_deleted")
    @Setter
    private Boolean deleted;

    @JsonIgnore
    YnsDb currentDb;

    public YnsDocument(String docId) {
        setDocId(docId);
    }
    
    @Override
    public String getUniqueId() {
        return docId;
    }

    public YnsDocIdAndRev getDocIdAndRev() {
        return new YnsDocIdAndRev(getDocId(), getRev());
    }

    public void addAttachment(String name, YnsDocumentAttachment attachment) {
        attachments.put(name, attachment);
    }

    public YnsDocumentAttachment getAttachment(String name) {
        return attachments.get(name);
    }

    public YnsDocumentAttachment deleteAttachment(String name) {
        return attachments.remove(name);
    }

    /**
     * Indicates that this document has been deleted and previous revisions will be removed on next compaction run.
     */
    public boolean isDeleted() {
        return deleted == null ? false : deleted;
    }

    public YnsDb getCurrentDb() {
        return currentDb;
    }

    @Override
    public String toString() {
        return docId + " / " + rev;
    }
}