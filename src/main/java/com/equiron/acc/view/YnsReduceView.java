package com.equiron.acc.view;

import java.util.Collections;

import com.equiron.acc.YnsDb;
import com.equiron.acc.json.resultset.YnsReduceResultSet;
import com.equiron.acc.query.YnsReduceQuery;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class YnsReduceView<K, V> extends YnsAbstractView {
    public YnsReduceView(YnsDb ynsDb, String designName, String viewName, JavaType[] jts) {
        super(ynsDb, designName, viewName, jts);
    }

    public YnsReduceQuery<K, V> createQuery() {
        TypeFactory tf = TypeFactory.defaultInstance();

        return new YnsReduceQuery<>(ynsDb, viewUrl, tf.constructParametricType(YnsReduceResultSet.class, keyType, valueType));
    }
    
    @Override
    public void update() {
        createQuery().group().byKeys(Collections.emptyList()).asKey();
    }
}
