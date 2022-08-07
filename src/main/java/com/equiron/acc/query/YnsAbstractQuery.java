package com.equiron.acc.query;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsResponseHandler;
import com.equiron.acc.json.resultset.YnsAbstractResultSet;
import com.equiron.acc.json.resultset.YnsAbstractRow;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.fasterxml.jackson.databind.JavaType;

import lombok.SneakyThrows;

public abstract class YnsAbstractQuery<K, V, ROW extends YnsAbstractRow<K, V>, RS extends YnsAbstractResultSet<K, V, ROW>, T extends YnsAbstractQuery<K, V, ROW, RS, T>> {
    String viewUrl;

    JavaType resultSetType;

    YnsQueryObject<K> queryObject;

    YnsDb ynsDb;

    Class<T> derived;

    @SuppressWarnings("unchecked")
    public YnsAbstractQuery(YnsDb ynsDb, String viewUrl, JavaType resultSetType) {
        this.ynsDb = ynsDb;
        this.viewUrl = viewUrl;
        this.resultSetType = resultSetType;
        this.derived = (Class<T>) this.getClass();

        queryObject = new YnsQueryObject<>(ynsDb.getMapper());
    }

    /**
     * Return only documents that match the specified keys.
     */
    @SuppressWarnings("unchecked")
    public T byKeys(K key, K... keys) {
        Object[] full = new Object[keys.length + 1];

        System.arraycopy(keys, 0, full, 1, keys.length);

        full[0] = key;

        queryObject.setKeys((K[])full);

        return derived.cast(this);
    }

    public T byKeys(K[] keys) {
        queryObject.setKeys(keys);

        return derived.cast(this);
    }

    @SuppressWarnings("unchecked")
    public T byKeys(Collection<K> keys) {
        queryObject.setKeys((K[])keys.toArray());

        return derived.cast(this);
    }

    /**
     * Return only documents that match the specified key.
     */
    public T byKey(K key) {
        queryObject.setSetKey(true);

        queryObject.setKey(key);

        return derived.cast(this);
    }

    /**
     * Return the documents in descending by key order.
     */
    public T descending() {
        queryObject.setDescending(true);

        return derived.cast(this);
    }

    /**
     * Stop returning records when the specified key is reached.
     */
    public T endKey(K key) {
        queryObject.setSetEndKey(true);

        queryObject.setEndKey(key);

        return derived.cast(this);
    }

    /**
     * Return records starting with the specified key.
     */
    public T startKey(K key) {
        queryObject.setSetStartKey(true);

        queryObject.setStartKey(key);

        return derived.cast(this);
    }

    /**
     * Specifies whether the specified end key should not be included in the result.
     */
    public T nonInclusiveEnd() {
        queryObject.setInclusiveEnd(false);

        return derived.cast(this);
    }

    /**
     * Limit the number of the returned documents to the specified number.
     */
    public T limit(int limit) {
        queryObject.setLimit(limit);

        return derived.cast(this);
    }

    /**
     * Skip this number of records before starting to return the results.
     * The skip option should only be used with small values, as skipping a large range of documents this way is inefficient
     * (it scans the index from the startkey and then skips N elements, but still needs to read all the index values to do that).
     */
    public T skip(int skip) {
        queryObject.setSkip(skip);

        return derived.cast(this);
    }

    public List<ROW> asRows() {
        return executeRequest(rs -> rs.getRows());
    }

    public List<K> asKeys() {
        return executeRequest(rs -> rs.keys());
    }

    public List<V> asValues() {
        return executeRequest(rs ->rs.values());
    }

    public ROW asRow() {
        return executeRequest(rs -> rs.firstRow());
    }

    public RS asResultSet() {
        return executeRequest(Function.identity());
    }

    public K asKey() {
        return executeRequest(rs -> rs.firstKey());
    }

    public V asValue() {
        return executeRequest(rs -> rs.firstValue());
    }

    public Map<K, ROW> asMap() {
        return executeRequest(rs -> rs.map());
    }

    public Map<K, List<ROW>> asMultiMap() {
        return executeRequest(rs -> rs.multiMap());
    }

    @SneakyThrows
    protected <O> O executeRequest(final Function<RS, O> transformer) {
        try {
            OperationInfo opInfo = new OperationInfo(OperationType.QUERY, viewUrl.substring(viewUrl.lastIndexOf("/") + 1), 0, 0);
            
            ynsDb.getOperations().getSemaphore().acquire();
            
            try {
                HttpClientProviderResponse response;
                
                if (queryObject.getKeys() != null) {
                    response = ynsDb.getHttpClientProvider().post(viewUrl + queryObject.toQuery(), queryObject.jsonKeys());
                } else {
                    response = ynsDb.getHttpClientProvider().get(viewUrl + queryObject.toQuery());
                }
    
                return new YnsResponseHandler<>(response, resultSetType, transformer, ynsDb.getMapper(), opInfo, ynsDb.getOperations().getYnsOperationStats()).transform();
            } finally {
                ynsDb.getOperations().getSemaphore().release();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
