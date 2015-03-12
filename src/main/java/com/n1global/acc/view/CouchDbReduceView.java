package com.n1global.acc.view;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.resultset.CouchDbReduceResultSet;
import com.n1global.acc.query.CouchDbReduceQuery;

public class CouchDbReduceView<K, V> extends CouchDbAbstractView {
    public CouchDbReduceView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        super(couchDb, designName, viewName, jts);
    }

    public CouchDbReduceQuery<K, V> createQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        return new CouchDbReduceQuery<>(couchDb, viewUrl, tf.constructParametrizedType(CouchDbReduceResultSet.class, CouchDbReduceResultSet.class, keyType, valueType));
    }
}
