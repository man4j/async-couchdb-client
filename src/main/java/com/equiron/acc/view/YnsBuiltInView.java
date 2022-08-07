package com.equiron.acc.view;

import java.util.Collections;

import com.equiron.acc.YnsDb;
import com.equiron.acc.json.YnsDocRev;
import com.equiron.acc.json.resultset.YnsMapResultSet;
import com.equiron.acc.query.YnsMapQuery;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Represents a built-in view of all documents in the database.
 */
public final class YnsBuiltInView implements YnsView {
    private final String viewUrl;

    private final YnsDb ynsDb;

    private final JavaType keyType;

    private final JavaType valueType;

    private final JavaType resultSetType;

    public YnsBuiltInView(YnsDb ynsDb) {
        this.ynsDb = ynsDb;

        TypeFactory tf = TypeFactory.defaultInstance();

        this.keyType = tf.constructType(String.class);
        this.valueType = tf.constructType(YnsDocRev.class);

        this.resultSetType = tf.constructParametricType(YnsMapResultSet.class, keyType, valueType);

        this.viewUrl = new UrlBuilder(ynsDb.getDbUrl()).addPathSegment("_all_docs").build();
    }

    public YnsMapQuery<String, YnsDocRev> createQuery() {
        return new YnsMapQuery<>(ynsDb, viewUrl, resultSetType);
    }

    @Override
    public void update() {
        createQuery().byKeys(Collections.emptyList()).asKey();
    }

    @Override
    public String getDesignName() {
        return "builtIn";
    }

    @Override
    public String getViewName() {
        return "builtIn";
    }
}
