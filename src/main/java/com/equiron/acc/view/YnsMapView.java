package com.equiron.acc.view;

import java.util.Collections;

import com.equiron.acc.YnsDb;
import com.equiron.acc.json.resultset.YnsMapResultSet;
import com.equiron.acc.query.YnsMapQuery;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class YnsMapView<K, V> extends YnsAbstractView {
    public YnsMapView(YnsDb ynsDb, String designName, String viewName, JavaType[] jts) {
        super(ynsDb, designName, viewName, jts);
    }

    public YnsMapQuery<K, V> createQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        return new YnsMapQuery<>(ynsDb, viewUrl, tf.constructParametricType(YnsMapResultSet.class, keyType, valueType));
    }

    @Override
    public void update() {
        createQuery().byKeys(Collections.emptyList()).asId();
    }
}