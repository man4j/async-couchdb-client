package com.equiron.acc.view;

import java.util.Collections;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.resultset.CouchDbReduceResultSet;
import com.equiron.acc.query.CouchDbReduceQuery;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class CouchDbReduceView<K, V> extends CouchDbAbstractView {
    public CouchDbReduceView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        super(couchDb, designName, viewName, jts);
    }

    public CouchDbReduceQuery<K, V> createQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        return new CouchDbReduceQuery<>(couchDb, viewUrl, tf.constructParametricType(CouchDbReduceResultSet.class, keyType, valueType));
    }
    
    @Override
    public void update() {
        createQuery().byKeys(Collections.emptyList()).asKey();
    }
}
