package com.n1global.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbViewInfo {
    private String signature;

    private String language;

    @JsonProperty("disk_size")
    private long diskSize;

    @JsonProperty("data_size")
    private long dataSize;

    @JsonProperty("updater_running")
    private boolean updaterRunning;

    @JsonProperty("compact_running")
    private boolean compactRunning;

    @JsonProperty("waiting_commit")
    private boolean waitingCommit;

    @JsonProperty("waiting_clients")
    private int waitingClients;

    @JsonProperty("update_seq")
    private long updateSeq;

    @JsonProperty("purge_seq")
    private long purgeSeq;

    /**
     * @return the MD5 representation of the views of a design document.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * @return language of the views used.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return size in Bytes of the views on disk.
     */
    public long getDiskSize() {
        return diskSize;
    }

    /**
     * @return size in Bytes of the actual data.
     */
    public long getDataSize() {
        return dataSize;
    }

    /**
     * Indicates if an update process is running.
     */
    public boolean isUpdaterRunning() {
        return updaterRunning;
    }

    /**
     * Indicates if view compaction is running.
     */
    public boolean isCompactRunning() {
        return compactRunning;
    }

    /**
     * Indicates if this view is ahead of db commits or not.
     */
    public boolean isWaitingCommit() {
        return waitingCommit;
    }

    /**
     * @return how many clients are waiting on views of this design document.
     */
    public int getWaitingClients() {
        return waitingClients;
    }

    /**
     * @return the update sequence of the corresponding database that has been indexed.
     */
    public long getUpdateSeq() {
        return updateSeq;
    }

    /**
     * @return the purge sequence that has been processed.
     */
    public long getPurgeSeq() {
        return purgeSeq;
    }

    @Override
    public String toString() {
        return "CouchDbViewInfo [signature=" + signature + ", language=" + language + ", diskSize=" + diskSize
                + ", dataSize=" + dataSize + ", updaterRunning=" + updaterRunning + ", compactRunning="
                + compactRunning + ", waitingCommit=" + waitingCommit + ", waitingClients=" + waitingClients
                + ", updateSeq=" + updateSeq + ", purgeSeq=" + purgeSeq + "]";
    }
}
