package com.n1global.acc;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import com.n1global.acc.util.ExceptionHandler;
import com.n1global.acc.util.FutureUtils;
import com.n1global.acc.util.UrlBuilder;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class CouchDbDirectUpdater {
    CouchDb couchDb;

    String handlerName;

    String designName;

    private CouchDbDirectUpdaterAsyncOperations asyncOps = new CouchDbDirectUpdaterAsyncOperations();

    public CouchDbDirectUpdater(CouchDb couchDb, String handlerName, String designDocName) {
        this.couchDb = couchDb;
        this.handlerName = handlerName;
        this.designName = designDocName;
    }

    public CouchDbDirectUpdaterAsyncOperations async() {
        return asyncOps;
    }

    public class CouchDbDirectUpdaterAsyncOperations {
        /**
         * Invoke a server-side update handler.
         */
        public CompletableFuture<Response> update(String docId, Map<String, String> params) {
            try {
                UrlBuilder urlBuilder = new UrlBuilder(couchDb.getDbUrl()).addPathSegment("_design")
                                                                          .addPathSegment(designName)
                                                                          .addPathSegment("_update")
                                                                          .addPathSegment(handlerName);
                String method = "POST";

                if (docId != null && !docId.isEmpty()) {
                    urlBuilder.addPathSegment(docId);

                    method = "PUT";
                }

                String stringBody = null;

                if (params != null && !params.isEmpty()) {
                    StringBuilder body = new StringBuilder();

                    for (Entry<String, String> e : params.entrySet()) {
                        body.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(e.getValue(), "UTF-8"))
                            .append("&");
                    }

                    stringBody = body.toString();

                    stringBody = stringBody.substring(0, stringBody.length() - 1);
                }

                BoundRequestBuilder builder = couchDb.getConfig()
                                                     .getHttpClient()
                                                     .prepareRequest(couchDb.prototype)
                                                     .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                                                     .setMethod(method)
                                                     .setUrl(urlBuilder.build());

                if (stringBody != null) {
                    builder.setBody(stringBody);
                }

                return FutureUtils.toCompletable(builder.execute());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Invoke a server-side update handler.
         */
        public CompletableFuture<Response> update() {
            return update(null, new HashMap<String, String>());
        }

        /**
         * Invoke a server-side update handler.
         */
        public CompletableFuture<Response> update(String docId) {
            return update(docId, new HashMap<String, String>());
        }

        /**
         * Invoke a server-side update handler.
         */
        public CompletableFuture<Response> update(Map<String, String> params) {
            return update(null, params);
        }
    }

    /**
     * Invoke a server-side update handler.
     */
    public Response update() {
        return update(null, new HashMap<String, String>());
    }

    /**
     * Invoke a server-side update handler.
     */
    public Response update(String docId) {
        return update(docId, new HashMap<String, String>());
    }

    /**
     * Invoke a server-side update handler.
     */
    public Response update(Map<String, String> params) {
        return update(null, params);
    }

    /**
     * Invoke a server-side update handler.
     */
    public Response update(String docId, Map<String, String> params) {
        return ExceptionHandler.handleFutureResult(asyncOps.update(docId, params));
    }
}
