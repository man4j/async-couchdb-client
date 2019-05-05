package com.equiron.acc.view;

import java.util.Map;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocRev;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.resultset.CouchDbMapResultSet;
import com.equiron.acc.json.resultset.CouchDbMapResultSetWithDocs;
import com.equiron.acc.query.CouchDbMapQuery;
import com.equiron.acc.query.CouchDbMapQueryWithDocs;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Represents a built-in view of all documents in the database.
 */
public final class CouchDbBuiltInView {
    private String viewUrl;

    private CouchDb couchDb;

    private JavaType keyType;

    private JavaType valueType;

    private JavaType resultSetType;

    public CouchDbBuiltInView(CouchDb couchDb) {
        this.couchDb = couchDb;

        TypeFactory tf = TypeFactory.defaultInstance();

        keyType = tf.constructType(String.class);
        valueType = tf.constructType(CouchDbDocRev.class);

        resultSetType = tf.constructParametricType(CouchDbMapResultSet.class, keyType, valueType);

        this.viewUrl = new UrlBuilder(couchDb.getDbUrl()).addPathSegment("_all_docs").build();
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
    public <T extends CouchDbDocument> CouchDbMapQueryWithDocs<String, CouchDbDocRev, T> createDocQuery() {
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
    public CouchDbMapQueryWithDocs<String, CouchDbDocRev, Map<String, Object>> createRawDocQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        JavaType resultSetType = tf.constructParametricType(CouchDbMapResultSetWithDocs.class, keyType, valueType, tf.constructMapType(Map.class, String.class, Object.class));

        return new CouchDbMapQueryWithDocs<>(couchDb, viewUrl, resultSetType);
    }

    public CouchDbMapQuery<String, CouchDbDocRev> createQuery() {
        return new CouchDbMapQuery<>(couchDb, viewUrl, resultSetType);
    }
}
