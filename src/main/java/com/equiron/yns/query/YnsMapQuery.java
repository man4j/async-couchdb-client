package com.equiron.yns.query;

import java.util.List;
import java.util.function.Function;

import com.equiron.yns.YnsDb;
import com.equiron.yns.json.resultset.YnsMapResultSet;
import com.equiron.yns.json.resultset.YnsMapRow;
import com.fasterxml.jackson.databind.JavaType;

public class YnsMapQuery<K, V> extends YnsAbstractQuery<K, V, YnsMapRow<K, V>, YnsMapResultSet<K, V>, YnsMapQuery<K, V>> {
    String lastKeyDocId;

    K lastKey;

    int totalRows;

    static final int BATCH_SIZE = 1000;

    public YnsMapQuery(YnsDb ynsDb, String viewUrl, JavaType resultSetType) {
        super(ynsDb, viewUrl, resultSetType);
    }

    /**
     * Stop returning records when the specified document ID is reached.
     */
    public YnsMapQuery<K, V> endKeyDocId(String docId) {
        if (docId == null) throw new IllegalStateException("The end key for document id cannot be null or empty");

        queryObject.setEndKeyDocId(docId);

        return this;
    }

    /**
     * Return records starting with the specified document ID.
     */
    public YnsMapQuery<K, V> startKeyDocId(String docId) {
        if (docId == null) throw new IllegalStateException("The start key for document id cannot be null or empty");

        queryObject.setStartKeyDocId(docId);

        return this;
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

    @Override
    protected <O> O executeRequest(final Function<YnsMapResultSet<K, V>, O> transformer) {
        Function<YnsMapResultSet<K, V>, O> delegate = rs -> {
            if (!rs.ids().isEmpty()) {
                lastKeyDocId = rs.ids().get(rs.ids().size() - 1);
                lastKey = rs.keys().get(rs.keys().size() - 1);
                totalRows = rs.getTotalRows();
            }

            return transformer.apply(rs);
        };

        return super.executeRequest(delegate);
    }

    public List<String> asIds() {
        return executeRequest(rs -> rs.ids());
    }

    public String asId() {
        return executeRequest(rs -> rs.firstId());
    }

    public YnsIterable<YnsMapRow<K, V>> asRowIterator(int batchSize) {
        return new YnsIterator<>(q -> q.asRows(), batchSize, derived.cast(this));
    }

    public YnsIterable<YnsMapRow<K, V>> asRowIterator() {
        return asRowIterator(BATCH_SIZE);
    }

    public YnsIterable<String> asIdIterator(int batchSize) {
        return new YnsIterator<>(q -> q.asIds(), batchSize, derived.cast(this));
    }

    public YnsIterable<String> asIdIterator() {
        return asIdIterator(BATCH_SIZE);
    }

    public YnsIterable<K> asKeyIterator(int batchSize) {
        return new YnsIterator<>(q -> q.asKeys(), batchSize, derived.cast(this));
    }

    public YnsIterable<K> asKeyIterator() {
        return asKeyIterator(BATCH_SIZE);
    }

    public YnsIterable<V> asValueIterator(int batchSize) {
        return new YnsIterator<>(q -> q.asValues(), batchSize, derived.cast(this));
    }

    public YnsIterable<V> asValueIterator() {
        return asValueIterator(BATCH_SIZE);
    }
}
