package com.n1global.acc.query;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbDocumentAccessor;
import com.n1global.acc.json.resultset.CouchDbMapResultSetWithDocs;
import com.n1global.acc.json.resultset.CouchDbMapRowWithDoc;
import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.Function;
import com.ning.http.client.ListenableFuture;

public class CouchDbMapQueryWithDocs<K, V, D> extends CouchDbAbstractMapQuery<K, V, CouchDbMapRowWithDoc<K, V, D>, CouchDbMapResultSetWithDocs<K, V, D>, CouchDbMapQueryWithDocs<K, V, D>> {
    private CouchDbMapQueryWithDocsAsyncOperations asyncOps = new CouchDbMapQueryWithDocsAsyncOperations();

    public CouchDbMapQueryWithDocs(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);

        queryObject.setIncludeDocs(true);
    }

    public class CouchDbMapQueryWithDocsAsyncOperations extends CouchDbAbstractMapQueryAsyncOperations {
        public ListenableFuture<List<D>> asDocs() {
            Function<CouchDbMapResultSetWithDocs<K, V, D>, List<D>> transformer = new Function<CouchDbMapResultSetWithDocs<K, V, D>, List<D>>() {
                @Override
                public List<D> apply(CouchDbMapResultSetWithDocs<K, V, D> input) {
                    return input.docs();
                }
            };

            return executeRequest(transformer);
        }

        public ListenableFuture<D> asDoc() {
            Function<CouchDbMapResultSetWithDocs<K, V, D>, D> transformer = new Function<CouchDbMapResultSetWithDocs<K, V, D>, D>() {
                @Override
                public D apply(CouchDbMapResultSetWithDocs<K, V, D> input) {
                    return input.firstDoc();
                }
            };

            return executeRequest(transformer);
        }

        @Override
        protected <T> ListenableFuture<T> executeRequest(final Function<CouchDbMapResultSetWithDocs<K, V, D>, T> transformer) {
            Function<CouchDbMapResultSetWithDocs<K, V, D>, T> delegate = new Function<CouchDbMapResultSetWithDocs<K, V, D>, T>() {
                @Override
                public T apply(CouchDbMapResultSetWithDocs<K, V, D> input) {
                    for (D d : input.docs()) {
                        if (d instanceof CouchDbDocument) {//maybe Map
                            new CouchDbDocumentAccessor((CouchDbDocument) d).setCurrentDb(couchDb);
                        }
                    }

                    return transformer.apply(input);
                }
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

    public CouchDbIterable<D> asDocsIterator(int batchSize) {
        return new CouchDbIterator<>(new Function<CouchDbMapQueryWithDocs<K, V, D>, List<D>>() {
            @Override
            public List<D> apply(CouchDbMapQueryWithDocs<K, V, D> view) {
                return view.asDocs();
            }
        }, batchSize, this);
    }

    public CouchDbIterable<D> asDocsIterator() {
        return asDocsIterator(BATCH_SIZE);
    }
}
