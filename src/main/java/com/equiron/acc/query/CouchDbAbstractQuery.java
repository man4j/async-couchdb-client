package com.equiron.acc.query;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbResponseHandler;
import com.equiron.acc.json.resultset.CouchDbAbstractResultSet;
import com.equiron.acc.json.resultset.CouchDbAbstractRow;
import com.equiron.acc.profiler.OperationInfo;
import com.equiron.acc.profiler.OperationType;
import com.equiron.acc.provider.HttpClientProviderResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.rainerhahnekamp.sneakythrow.Sneaky;

public abstract class CouchDbAbstractQuery<K, V, ROW extends CouchDbAbstractRow<K, V>, RS extends CouchDbAbstractResultSet<K, V, ROW>, T extends CouchDbAbstractQuery<K, V, ROW, RS, T>> {
    String viewUrl;

    JavaType resultSetType;

    CouchDbQueryObject<K> queryObject;

    CouchDb couchDb;

    Class<T> derived;

    @SuppressWarnings("unchecked")
    public CouchDbAbstractQuery(CouchDb couchDb, String viewUrl, JavaType resultSetType) {
        this.couchDb = couchDb;
        this.viewUrl = viewUrl;
        this.resultSetType = resultSetType;
        this.derived = (Class<T>) this.getClass();

        queryObject = new CouchDbQueryObject<>(couchDb.getMapper());
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
     * Whether or not the view results should be returned from a stable set of shards. 
     */
    public T stable() {
        queryObject.setStable(true);

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

    protected <O> O executeRequest(final Function<RS, O> transformer) {
        try {
            OperationInfo opInfo = new OperationInfo(OperationType.QUERY, viewUrl.substring(viewUrl.lastIndexOf("/") + 1), 0, 0);
            
            Sneaky.sneak(() -> { couchDb.getOperations().getSemaphore().acquire(); return true;} );
            
            try {
                HttpClientProviderResponse response;
                
                if (queryObject.getKeys() != null) {
                    response = couchDb.getHttpClientProvider().post(viewUrl + queryObject.toQuery(), queryObject.jsonKeys());
                } else {
                    response = couchDb.getHttpClientProvider().get(viewUrl + queryObject.toQuery());
                }
    
                return new CouchDbResponseHandler<>(response, resultSetType, transformer, couchDb.getMapper(), opInfo, couchDb.getOperations().getCouchDbOperationStats()).transform();
            } finally {
                couchDb.getOperations().getSemaphore().release();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
