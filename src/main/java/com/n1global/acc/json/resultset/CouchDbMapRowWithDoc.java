package com.n1global.acc.json.resultset;

public class CouchDbMapRowWithDoc<K, V, D> extends CouchDbMapRow<K, V> {
    private D doc;

    public D getDoc() {
        return doc;
    }
}
