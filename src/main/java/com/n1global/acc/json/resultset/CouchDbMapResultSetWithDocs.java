package com.n1global.acc.json.resultset;

import java.util.ArrayList;
import java.util.List;

public class CouchDbMapResultSetWithDocs<K, V, D> extends CouchDbAbstractMapResultSet<K, V, CouchDbMapRowWithDoc<K, V, D>> {
    public List<D> docs() {
        List<D> values = new ArrayList<>();

        for (CouchDbMapRowWithDoc<K, V, D> row : getRows()) {
            values.add(row.getDoc());
        }

        return values;
    }

    public D firstDoc() {
        return getRows().isEmpty() ? null : getRows().get(0).getDoc();
    }
}
