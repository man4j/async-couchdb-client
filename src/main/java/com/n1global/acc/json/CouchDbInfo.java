package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbInfo {
    @JsonProperty("db_name")
    private String dbName;

    @JsonProperty("doc_count")
    private long docCount;

    @JsonProperty("doc_del_count")
    private long docDelCount;

    @JsonProperty("update_seq")
    private long updateSeq;

    @JsonProperty("purge_seq")
    private long purgeSeq;

    @JsonProperty("compact_running")
    private boolean compactRunning;

    @JsonProperty("disk_size")
    private long diskSize;

    @JsonProperty("data_size")
    private long dataSize;

    @JsonProperty("instance_start_time")
    private long instanceStartTime;

    @JsonProperty("disk_format_version")
    private int diskFormatVersion;

    @JsonProperty("committed_update_seq")
    private long committedUpdateSeq;

    /**
     * @return name of the database.
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @return number of documents (including design documents) in the database.
     */
    public long getDocCount() {
        return docCount;
    }

    /**
     * When you delete a document the database will create a new revision which contains the _id and _rev fields
     * as well as a deleted flag. This revision will remain even after a database compaction so that the deletion
     * can be replicated. Deleted documents, like non-deleted documents, can affect view build times, PUT and
     * DELETE requests time and size of database on disk, since they increase the size of the B+Tree's. You can
     * see the number of deleted documents in this field. If your use case creates lots of deleted documents
     * (for example, if you are storing short-term data like logfile entries, message queues, etc), you might want to
     * periodically switch to a new database and delete the old one (once the entries in it have all expired).
     */
    public long getDocDelCount() {
        return docDelCount;
    }

    /**
     * @return current number of updates to the database.
     */
    public long getUpdateSeq() {
        return updateSeq;
    }

    /**
     * @return number of purge operations.
     */
    public long getPurgeSeq() {
        return purgeSeq;
    }

    /**
     * Indicates, if a compaction is running.
     */
    public boolean isCompactRunning() {
        return compactRunning;
    }

    /**
     * @return current size in Bytes of the database (Note: Size of views indexes on disk are not included).
     */
    public long getDiskSize() {
        return diskSize;
    }

    /**
     * @return current size in Bytes of the data.
     */
    public long getDataSize() {
        return dataSize;
    }

    /**
     * @return timestamp of CouchDBs start time (int in ms).
     */
    public long getInstanceStartTime() {
        return instanceStartTime;
    }

    /**
     * @return current version of the internal database format on disk.
     */
    public int getDiskFormatVersion() {
        return diskFormatVersion;
    }

    public long getCommittedUpdateSeq() {
        return committedUpdateSeq;
    }

    @Override
    public String toString() {
        return "CouchDbInfo [dbName=" + dbName + ", docCount=" + docCount + ", docDelCount=" + docDelCount
                + ", updateSeq=" + updateSeq + ", purgeSeq=" + purgeSeq + ", compactRunning=" + compactRunning
                + ", diskSize=" + diskSize + ", dataSize=" + dataSize + ", instanceStartTime=" + instanceStartTime
                + ", diskFormatVersion=" + diskFormatVersion + ", committedUpdateSeq=" + committedUpdateSeq + "]";
    }
}
