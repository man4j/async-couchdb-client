package com.equiron.acc.json.resultset;

import java.util.List;
import java.util.stream.Collectors;

public class CouchDbMapResultSetWithDocs<K, V, D> extends CouchDbAbstractMapResultSet<K, V, CouchDbMapRowWithDoc<K, V, D>> {
    public List<D> docs() {
        return getRows().stream().map(CouchDbMapRowWithDoc::getDoc).collect(Collectors.toList());
    }

    public D firstDoc() {
        return getRows().isEmpty() ? null : getRows().get(0).getDoc();
    }
}
