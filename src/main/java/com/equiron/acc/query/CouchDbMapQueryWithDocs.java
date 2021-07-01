package com.equiron.acc.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbDocumentAccessor;
import com.equiron.acc.json.resultset.CouchDbMapResultSetWithDocs;
import com.equiron.acc.json.resultset.CouchDbMapRowWithDoc;
import com.fasterxml.jackson.databind.JavaType;

public class CouchDbMapQueryWithDocs<K, V, D> extends CouchDbAbstractMapQuery<K, V, CouchDbMapRowWithDoc<K, V, D>, CouchDbMapResultSetWithDocs<K, V, D>, CouchDbMapQueryWithDocs<K, V, D>> {
    public CouchDbMapQueryWithDocs(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);

        queryObject.setIncludeDocs(true);
    }

    public List<D> asDocs() {
        return executeRequest(rs -> rs.docs());
    }

    public D asDoc() {
        return executeRequest(rs -> rs.firstDoc());
    }

    @Override
    protected <T> T executeRequest(final Function<CouchDbMapResultSetWithDocs<K, V, D>, T> transformer) {
        Function<CouchDbMapResultSetWithDocs<K, V, D>, T> delegate = rs -> {
            for (D d : rs.docs()) {
                if (d instanceof CouchDbDocument) {//maybe Map
                    new CouchDbDocumentAccessor((CouchDbDocument) d).setCurrentDb(couchDb);
                }
            }

            return transformer.apply(rs);
        };

        return super.executeRequest(delegate);
    }
    
    /**
     * Include attachments into the document.
     */
    public CouchDbMapQueryWithDocs<K, V, D> includeAttachments() {
        queryObject.setAttachments(true);

        return this;
    }

    public CouchDbIterable<D> asDocIterator(int batchSize) {
        return new CouchDbIterator<>(view -> view.asDocs(), batchSize, this);
    }

    public CouchDbIterable<D> asDocIterator() {
        return asDocIterator(BATCH_SIZE);
    }
    
    public Map<K, D> asDocMap() {
        return asMap().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getDoc(), (e1, e2) -> e1, LinkedHashMap::new));
    }
    
    public Map<K, List<D>> asDocMultiMap() {
        Map<K, List<D>> map = new LinkedHashMap<>();
        
        asMultiMap().entrySet().forEach(e -> {
            List<D> docs = e.getValue().stream().map(CouchDbMapRowWithDoc::getDoc).collect(Collectors.toList());
            
            map.put(e.getKey(), docs);
        });
        
        return map;
    }
}
