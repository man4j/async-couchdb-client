package com.equiron.acc.query;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.resultset.CouchDbReduceResultSet;
import com.equiron.acc.json.resultset.CouchDbReduceRow;
import com.fasterxml.jackson.databind.JavaType;

public class CouchDbReduceQuery<K, V> extends CouchDbAbstractQuery<K, V, CouchDbReduceRow<K, V>, CouchDbReduceResultSet<K, V>, CouchDbReduceQuery<K, V>> {
    private CouchDbAbstractQueryAsyncOperations asyncOps = new CouchDbAbstractQueryAsyncOperations();

    public CouchDbReduceQuery(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);

        queryObject.setReduce(true);
    }

    /**
     * The group option controls whether the reduce function reduces to a set of distinct keys or to a single result row.
     * Don't specify both group and groupLevel; the second one given will override the first.
     */
    public CouchDbReduceQuery<K, V> group() {
        queryObject.setGroup(true);

        return this;
    }

    /**
     * Specify the group level to be used.
     * Don't specify both group and groupLevel; the second one given will override the first.
     */
    public CouchDbReduceQuery<K, V> groupLevel(int level) {
        queryObject.setGroupLevel(level);

        return this;
    }

    @Override
    public CouchDbAbstractQueryAsyncOperations async() {
        return asyncOps;
    }
}
