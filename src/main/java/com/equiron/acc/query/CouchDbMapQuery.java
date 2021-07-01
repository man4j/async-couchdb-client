package com.equiron.acc.query;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.resultset.CouchDbAbstractMapResultSet;
import com.equiron.acc.json.resultset.CouchDbMapRow;
import com.fasterxml.jackson.databind.JavaType;

public class CouchDbMapQuery<K, V> extends CouchDbAbstractMapQuery<K, V, CouchDbMapRow<K, V>, CouchDbAbstractMapResultSet<K, V, CouchDbMapRow<K, V>>, CouchDbMapQuery<K, V>> {
    public CouchDbMapQuery(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);
    }
}
