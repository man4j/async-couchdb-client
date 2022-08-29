package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
public class YnsViewInfo {
    /**
     * @return the MD5 representation of the views of a design document.
     */
    private String signature;

    /**
     * @return language of the views used.
     */
    private String language;

    /**
     * @return size in Bytes of the views on disk.
     */
    @JsonProperty("disk_size")
    private long diskSize;

    /**
     * @return size in Bytes of the actual data.
     */
    @JsonProperty("data_size")
    private long dataSize;

    /**
     * Indicates if an update process is running.
     */
    @JsonProperty("updater_running")
    private boolean updaterRunning;

    /**
     * Indicates if view compaction is running.
     */
    @JsonProperty("compact_running")
    private boolean compactRunning;

    /**
     * Indicates if this view is ahead of db commits or not.
     */
    @JsonProperty("waiting_commit")
    private boolean waitingCommit;

    /**
     * @return how many clients are waiting on views of this design document.
     */
    @JsonProperty("waiting_clients")
    private int waitingClients;

    /**
     * @return the update sequence of the corresponding database that has been indexed.
     */
    @JsonProperty("update_seq")
    private long updateSeq;

    /**
     * @return the purge sequence that has been processed.
     */
    @JsonProperty("purge_seq")
    private long purgeSeq;
}