package com.n1global.acc.query;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JavaType;
import com.n1global.acc.CouchDb;
import com.n1global.acc.json.resultset.CouchDbAbstractMapResultSet;
import com.n1global.acc.json.resultset.CouchDbMapRow;
import com.n1global.acc.util.ExceptionHandler;
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
            Function<RS, O> delegate = rs -> {
                if (!rs.ids().isEmpty()) {
                    lastKeyDocId = rs.ids().get(rs.ids().size() - 1);
                    lastKey = rs.keys().get(rs.keys().size() - 1);
                    totalRows = rs.getTotalRows();
                }

                return transformer.apply(rs);
            };

            return super.executeRequest(delegate);
        }

        public ListenableFuture<List<String>> asIds() {
            return executeRequest(rs -> rs.ids());
        }

        public ListenableFuture<String> asId() {
            return executeRequest(rs -> rs.firstId());
        }
    }

    @Override
    public abstract CouchDbAbstractMapQueryAsyncOperations async();

    public CouchDbIterable<ROW> asRowIterator(int batchSize) {
        return new CouchDbIterator<>(q -> q.asRows(), batchSize, derived.cast(this));
    }

    public CouchDbIterable<ROW> asRowIterator() {
        return asRowIterator(BATCH_SIZE);
    }

    public CouchDbIterable<String> asIdIterator(int batchSize) {
        return new CouchDbIterator<>(q -> q.asIds(), batchSize, derived.cast(this));
    }

    public CouchDbIterable<String> asIdIterator() {
        return asIdIterator(BATCH_SIZE);
    }

    public CouchDbIterable<K> asKeyIterator(int batchSize) {
        return new CouchDbIterator<>(q -> q.asKeys(), batchSize, derived.cast(this));
    }

    public CouchDbIterable<K> asKeyIterator() {
        return asKeyIterator(BATCH_SIZE);
    }

    public CouchDbIterable<V> asValueIterator(int batchSize) {
        return new CouchDbIterator<>(q -> q.asValues(), batchSize, derived.cast(this));
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
