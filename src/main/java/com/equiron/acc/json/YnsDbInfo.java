package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
public class YnsDbInfo {
    /**
     * @return name of the database.
     */
    @JsonProperty("db_name")
    private String dbName;

    /**
     * @return number of documents (including design documents) in the database.
     */
    @JsonProperty("doc_count")
    private long docCount;

    /**
     * When you delete a document the database will create a new revision which contains the _id and _rev fields
     * as well as a deleted flag. This revision will remain even after a database compaction so that the deletion
     * can be replicated. Deleted documents, like non-deleted documents, can affect view build times, PUT and
     * DELETE requests time and size of database on disk, since they increase the size of the B+Tree's. You can
     * see the number of deleted documents in this field. If your use case creates lots of deleted documents
     * (for example, if you are storing short-term data like logfile entries, message queues, etc), you might want to
     * periodically switch to a new database and delete the old one (once the entries in it have all expired).
     */
    @JsonProperty("doc_del_count")
    private long docDelCount;
    
    /**
     * @return opaque string that describes the state of the database. Do not rely on this string for counting the number of updates.
     */
    @JsonProperty("update_seq")
    private String updateSeq;

    private Size sizes;
    
    private YnsClusterInfo cluster;

    /**
     * @return timestamp of CouchDBs start time (int in ms).
     */
    @JsonProperty("instance_start_time")
    private long instanceStartTime;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class Size {
        /**
         * @return size of live data inside the database, in bytes.
         */
        private long active;

        /**
         * @return size of the database file on disk in bytes. Views indexes are not included in the calculation.
         */
        private long file;
        
        /**
         * @return uncompressed size of database contents in bytes.
         */
        private long external;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class YnsClusterInfo {
        /**
         * @return number of database shards to maintain.
         */
        private int q;

        /**
         * @return number of copies of each document to distribute.
         */
        private int n;

        /**
         * @return size of a write quorum.
         */
        private int w;

        /**
         * @return size of a read quorum.
         */
        private int r;
    }
}
