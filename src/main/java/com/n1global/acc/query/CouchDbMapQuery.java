package com.n1global.acc.query;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.resultset.CouchDbAbstractMapResultSet;
import com.n1global.acc.json.resultset.CouchDbMapRow;

public class CouchDbMapQuery<K, V> extends CouchDbAbstractMapQuery<K, V, CouchDbMapRow<K, V>, CouchDbAbstractMapResultSet<K, V, CouchDbMapRow<K, V>>, CouchDbMapQuery<K, V>> {
    private CouchDbAbstractMapQueryAsyncOperations asyncOps = new CouchDbAbstractMapQueryAsyncOperations();

    public CouchDbMapQuery(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);
    }

    @Override
    public CouchDbAbstractMapQueryAsyncOperations async() {
        return asyncOps;
    }
}
