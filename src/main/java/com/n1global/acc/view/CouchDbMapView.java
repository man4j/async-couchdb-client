package com.n1global.acc.view;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.resultset.CouchDbMapResultSet;
import com.n1global.acc.json.resultset.CouchDbMapResultSetWithDocs;
import com.n1global.acc.query.CouchDbMapQuery;
import com.n1global.acc.query.CouchDbMapQueryWithDocs;

public class CouchDbMapView<K, V> extends CouchDbAbstractView {
    public CouchDbMapView(CouchDb couchDb, String designName, String viewName, JavaType[] jts) {
        super(couchDb, designName, viewName, jts);
    }

    /**
     * The result set will include the associated document.
     * However, the user should keep in mind that there is a race condition when using this option.
     * It is possible that between reading the view data and fetching the corresponding document that the document has changed.
     * If you want to alleviate such concerns you should emit an object with a _rev attribute as in emit(key, {"_rev": doc._rev}).
     * This alleviates the race condition but leaves the possibility that the returned document has been deleted (in which case,
     * it includes the "_deleted": true attribute). Note: include_docs will cause a single document lookup per returned view result row.
     * This adds significant strain on the storage system if you are under high load or return a lot of rows per request.
     * If you are concerned about this, you can emit the full doc in each row; this will increase view index time and space requirements,
     * but will make view reads optimally fast.
    */
    public <T extends CouchDbDocument> CouchDbMapQueryWithDocs<K, V, T> createDocQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        JavaType resultSetType = tf.constructParametricType(CouchDbMapResultSetWithDocs.class, keyType, valueType, tf.constructType(CouchDbDocument.class));

        return new CouchDbMapQueryWithDocs<>(couchDb, viewUrl, resultSetType);
    }

    /**
     * The result set will include the associated document.
     * However, the user should keep in mind that there is a race condition when using this option.
     * It is possible that between reading the view data and fetching the corresponding document that the document has changed.
     * If you want to alleviate such concerns you should emit an object with a _rev attribute as in emit(key, {"_rev": doc._rev}).
     * This alleviates the race condition but leaves the possibility that the returned document has been deleted (in which case,
     * it includes the "_deleted": true attribute). Note: include_docs will cause a single document lookup per returned view result row.
     * This adds significant strain on the storage system if you are under high load or return a lot of rows per request.
     * If you are concerned about this, you can emit the full doc in each row; this will increase view index time and space requirements,
     * but will make view reads optimally fast.
    */
    public CouchDbMapQueryWithDocs<K, V, Map<String, Object>> createRawDocQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        JavaType resultSetType = tf.constructParametricType(CouchDbMapResultSetWithDocs.class, keyType, valueType, tf.constructMapType(Map.class, String.class, Object.class));

        return new CouchDbMapQueryWithDocs<>(couchDb, viewUrl, resultSetType);
    }

    public CouchDbMapQuery<K, V> createQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        return new CouchDbMapQuery<>(couchDb, viewUrl, tf.constructParametricType(CouchDbMapResultSet.class, keyType, valueType));
    }
}