package com.n1global.acc.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbDocumentAccessor;
import com.n1global.acc.json.resultset.CouchDbMapResultSetWithDocs;
import com.n1global.acc.json.resultset.CouchDbMapRowWithDoc;
import com.n1global.acc.util.ExceptionHandler;

public class CouchDbMapQueryWithDocs<K, V, D> extends CouchDbAbstractMapQuery<K, V, CouchDbMapRowWithDoc<K, V, D>, CouchDbMapResultSetWithDocs<K, V, D>, CouchDbMapQueryWithDocs<K, V, D>> {
    private CouchDbMapQueryWithDocsAsyncOperations asyncOps = new CouchDbMapQueryWithDocsAsyncOperations();

    public CouchDbMapQueryWithDocs(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);

        queryObject.setIncludeDocs(true);
    }

    public class CouchDbMapQueryWithDocsAsyncOperations extends CouchDbAbstractMapQueryAsyncOperations {
        public CompletableFuture<List<D>> asDocs() {
            return executeRequest(rs -> rs.docs());
        }

        public CompletableFuture<D> asDoc() {
            return executeRequest(rs -> rs.firstDoc());
        }

        @Override
        protected <T> CompletableFuture<T> executeRequest(final Function<CouchDbMapResultSetWithDocs<K, V, D>, T> transformer) {
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
    }

    @Override
    public CouchDbMapQueryWithDocsAsyncOperations async() {
        return asyncOps;
    }

    public List<D> asDocs() {
        return ExceptionHandler.handleFutureResult(async().asDocs());
    }

    public D asDoc() {
        return ExceptionHandler.handleFutureResult(async().asDoc());
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
