package com.n1global.acc.json.taskinfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbReplicationTaskInfo extends CouchDbTaskInfo {
    @JsonProperty("replication_id")
    private String replicationId;

    @JsonProperty("checkpointed_source_seq")
    private int checkpointedSourceSeq;

    private boolean continuous;

    @JsonProperty("doc_write_failures")
    private int docWriteFailures;

    @JsonProperty("docs_read")
    private int docsRead;

    @JsonProperty("docs_written")
    private int docsWritten;

    @JsonProperty("missing_revisions_found")
    private int missingRevisionsFound;

    @JsonProperty("revisions_checked")
    private int revisionsChecked;

    private String source;

    @JsonProperty("source_seq")
    private long sourceSeq;

    private String target;

    public String getReplicationId() {
        return replicationId;
    }

    public int getCheckpointedSourceSeq() {
        return checkpointedSourceSeq;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public int getDocWriteFailures() {
        return docWriteFailures;
    }

    public int getDocsRead() {
        return docsRead;
    }

    public int getDocsWritten() {
        return docsWritten;
    }

    public int getMissingRevisionsFound() {
        return missingRevisionsFound;
    }

    public int getRevisionsChecked() {
        return revisionsChecked;
    }

    public String getSource() {
        return source;
    }

    public long getSourceSeq() {
        return sourceSeq;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "CouchDbReplicationTaskInfo [replicationId=" + replicationId + ", checkpointedSourceSeq="
                + checkpointedSourceSeq + ", continuous=" + continuous + ", docWriteFailures=" + docWriteFailures
                + ", docsRead=" + docsRead + ", docsWritten=" + docsWritten + ", missingRevisionsFound="
                + missingRevisionsFound + ", revisionsChecked=" + revisionsChecked + ", source=" + source
                + ", sourceSeq=" + sourceSeq + ", target=" + target + ", getProgress()=" + getProgress()
                + ", getStartedOn()=" + getStartedOn() + ", getUpdatedOn()=" + getUpdatedOn() + "]";
    }
}
