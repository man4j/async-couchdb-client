package com.n1global.acc;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.n1global.acc.json.CouchDbBooleanResponse;
import com.n1global.acc.json.taskinfo.CouchDbTaskInfo;
import com.n1global.acc.transformer.CouchDbBooleanResponseTransformer;
import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.NoopFunction;
import com.n1global.acc.util.UrlBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;

public class CouchDbAdmin extends CouchDbBase {
    private CouchDbAdminAsyncOperations asyncOps = new CouchDbAdminAsyncOperations();
    
    private AsyncHttpClient httpClient;

    public CouchDbAdmin(CouchDbBaseConfig config) {
        super(config);
        
        httpClient = config.getHttpClient();
    }

    public class CouchDbAdminAsyncOperations {
        /**
         * Returns a list of databases on this server. The returned database names are unescaped, and may contain
         * characters that need to be properly escaped to be used as the database part in an URL. Most notably,
         * databases in subdirectories contain one or more slashes in their names, and these must be escaped as %2F
         * when used in URLs.
         */
        public ListenableFuture<List<String>> getListDbsAsync() {
            return httpClient.prepareRequest(prototype)
                             .setUrl(getConfig().getServerUrl() + "/_all_dbs")
                             .setMethod("GET")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<String>>() {/* empty */}, new NoopFunction<List<String>>(), mapper));
        }

        /**
         * Returns server statistics.
         */
        public ListenableFuture<Map<String, Object>> getStats() {
            return httpClient.prepareRequest(prototype)
                             .setUrl(getConfig().getServerUrl() +  "/_stats")
                             .setMethod("GET")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<Map<String, Object>>() {/* empty */}, new NoopFunction<Map<String, Object>>(), mapper));
        }

        /**
         * Returns a list of running tasks.
         */
        public ListenableFuture<List<CouchDbTaskInfo>> getActiveTasks() {
            return httpClient.prepareRequest(prototype)
                             .setUrl(getConfig().getServerUrl() +  "/_active_tasks")
                             .setMethod("GET")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<List<CouchDbTaskInfo>>() {/* empty */}, new NoopFunction<List<CouchDbTaskInfo>>(), mapper));
        }

        /**
         * Delete an existing database.
         */
        public ListenableFuture<Boolean> deleteDb(String dbName) {
            return httpClient.prepareRequest(prototype)
                             .setUrl(new UrlBuilder(getConfig().getServerUrl()).addPathSegment(dbName).build())
                             .setMethod("DELETE")
                             .execute(new CouchDbAsyncHandler<>(new TypeReference<CouchDbBooleanResponse>() {/* empty */}, new CouchDbBooleanResponseTransformer(), mapper));
        }
    }

    public CouchDbAdminAsyncOperations async() {
        return asyncOps;
    }

    /**
     * Returns a list of databases on this server. The returned database names are unescaped, and may contain
     * characters that need to be properly escaped to be used as the database part in an URL. Most notably,
     * databases in subdirectories contain one or more slashes in their names, and these must be escaped as %2F
     * when used in URLs.
     *
     * @return a list of databases on this server.
     */
    public List<String> getListDbs() {
        return ExceptionHandler.handleFutureResult(asyncOps.getListDbsAsync());
    }

    /**
     * @return server statistics.
     */
    public Map<String, Object> getStats() {
        return ExceptionHandler.handleFutureResult(asyncOps.getStats());
    }

    /**
     * @return a list of running tasks.
     */
    public List<CouchDbTaskInfo> getActiveTasks() {
        return ExceptionHandler.handleFutureResult(asyncOps.getActiveTasks());
    }

    /**
     * Delete an existing database.
     */
    public boolean deleteDb(String dbName) {
        return ExceptionHandler.handleFutureResult(asyncOps.deleteDb(dbName));
    }
}
