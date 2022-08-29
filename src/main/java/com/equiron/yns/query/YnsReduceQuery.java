package com.equiron.yns.query;

import com.equiron.yns.YnsDb;
import com.equiron.yns.json.resultset.YnsReduceResultSet;
import com.equiron.yns.json.resultset.YnsReduceRow;
import com.fasterxml.jackson.databind.JavaType;

public class YnsReduceQuery<K, V> extends YnsAbstractQuery<K, V, YnsReduceRow<K, V>, YnsReduceResultSet<K, V>, YnsReduceQuery<K, V>> {
    public YnsReduceQuery(YnsDb ynsDb, String viewUrl, JavaType resultSetType) {
        super(ynsDb, viewUrl, resultSetType);

        queryObject.setReduce(true);
    }

    /**
     * The group option controls whether the reduce function reduces to a set of distinct keys or to a single result row.
     * Don't specify both group and groupLevel; the second one given will override the first.
     */
    public YnsReduceQuery<K, V> group() {
        queryObject.setGroup(true);

        return this;
    }

    /**
     * Specify the group level to be used.
     * Don't specify both group and groupLevel; the second one given will override the first.
     */
    public YnsReduceQuery<K, V> groupLevel(int level) {
        queryObject.setGroupLevel(level);

        return this;
    }
}
