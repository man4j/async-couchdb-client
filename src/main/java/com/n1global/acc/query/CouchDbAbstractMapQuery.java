package com.n1global.acc.query;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.resultset.CouchDbAbstractMapResultSet;
import com.n1global.acc.json.resultset.CouchDbMapRow;
import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.Function;
import com.ning.http.client.ListenableFuture;

public abstract class CouchDbAbstractMapQuery<K, V, ROW extends CouchDbMapRow<K, V>, RS extends CouchDbAbstractMapResultSet<K, V, ROW>, T extends CouchDbAbstractMapQuery<K, V, ROW, RS, T>> extends CouchDbAbstractQuery<K, V, ROW, RS, T> {
    private String lastKeyDocId;

    private K lastKey;

    private int totalRows;

    static final int BATCH_SIZE = 1000;

    public CouchDbAbstractMapQuery(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        super(couchDb, viewUrl, resultSetType);
    }

    /**
     * Stop returning records when the specified document ID is reached.
     */
    public T endKeyDocId(String docId) {
        if (docId == null) throw new IllegalStateException("The end key for document id cannot be null or empty");

        queryObject.setEndKeyDocId(docId);

        return derived.cast(this);
    }

    /**
     * Return records starting with the specified document ID.
     */
    public T startKeyDocId(String docId) {
        if (docId == null) throw new IllegalStateException("The start key for document id cannot be null or empty");

        queryObject.setStartKeyDocId(docId);

        return derived.cast(this);
    }

    String getLastKeyDocId() {
        return lastKeyDocId;
    }

    K getLastKey() {
        return lastKey;
    }

    int getTotalRows() {
        return totalRows;
    }

    public class CouchDbAbstractMapQueryAsyncOperations extends CouchDbAbstractQueryAsyncOperations {
        @Override
        protected <O> ListenableFuture<O> executeRequest(final Function<RS, O> transformer) {
            Function<RS, O> delegate = new Function<RS, O>() {
                @Override
                public O apply(RS input) {
                    if (!input.ids().isEmpty()) {
                        lastKeyDocId = input.ids().get(input.ids().size() - 1);
                        lastKey = input.keys().get(input.keys().size() - 1);
                        totalRows = input.getTotalRows();
                    }

                    return transformer.apply(input);
                }
            };

            return super.executeRequest(delegate);
        }

        public ListenableFuture<List<String>> asIds() {
            Function<RS, List<String>> transformer = new Function<RS, List<String>>() {
                @Override
                public List<String> apply(RS input) {
                    return input.ids();
                }
            };

            return executeRequest(transformer);
        }

        public ListenableFuture<String> asId() {
            Function<RS, String> transformer = new Function<RS, String>() {
                @Override
                public String apply(RS input) {
                    return input.firstId();
                }
            };

            return executeRequest(transformer);
        }
    }

    @Override
    public abstract CouchDbAbstractMapQueryAsyncOperations async();

    public CouchDbIterable<ROW> asRowIterator(int batchSize) {
        return new CouchDbIterator<>(new Function<T, List<ROW>>() {
            @Override
            public List<ROW> apply(T query) {
                return query.asRows();
            }
        }, batchSize, derived.cast(this));
    }

    public CouchDbIterable<ROW> asRowIterator() {
        return asRowIterator(BATCH_SIZE);
    }

    public CouchDbIterable<String> asIdIterator(int batchSize) {
        return new CouchDbIterator<>(new Function<T, List<String>>() {
            @Override
            public List<String> apply(T query) {
                return query.asIds();
            }
        }, batchSize, derived.cast(this));
    }

    public CouchDbIterable<String> asIdIterator() {
        return asIdIterator(BATCH_SIZE);
    }

    public CouchDbIterable<K> asKeyIterator(int batchSize) {
        return new CouchDbIterator<>(new Function<T, List<K>>() {
            @Override
            public List<K> apply(T query) {
                return query.asKeys();
            }
        }, batchSize, derived.cast(this));
    }

    public CouchDbIterable<K> asKeyIterator() {
        return asKeyIterator(BATCH_SIZE);
    }

    public CouchDbIterable<V> asValueIterator(int batchSize) {
        return new CouchDbIterator<>(new Function<T, List<V>>() {
            @Override
            public List<V> apply(T query) {
                return query.asValues();
            }
        }, batchSize, derived.cast(this));
    }

    public CouchDbIterable<V> asValueIterator() {
        return asValueIterator(BATCH_SIZE);
    }

    public List<String> asIds() {
        return ExceptionHandler.handleFutureResult(async().asIds());
    }

    public String asId() {
        return ExceptionHandler.handleFutureResult(async().asId());
    }
}
