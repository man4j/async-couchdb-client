package com.n1global.acc.notification;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.n1global.acc.CouchDb;
import com.n1global.acc.CouchDbFilter;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbEvent;
import com.n1global.acc.util.UrlBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

public abstract class CouchDbEventListener<D extends CouchDbDocument> implements AutoCloseable {
    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private CouchDbEventAsyncHandler<D> asyncHandler;

    private Future<Void> messagingFuture;

    /**
     * For json serializing / deserializing. This object is thread-safe.
     */
    private final ObjectMapper mapper;

    private final Request prototype;

    private final AsyncHttpClient httpClient;

    private CouchDbNotificationConfig config;

    private CouchDb db;

    public CouchDbEventListener(CouchDb db) {
        this(db, null);
    }

    public CouchDbEventListener(CouchDb db, CouchDbNotificationConfig config) {
        this.db = db;
        this.mapper = db.getMapper();
        this.config = config != null ? config : new CouchDbNotificationConfig.Builder().build();
        this.httpClient = this.config.getHttpClient() != null ? this.config.getHttpClient() : db.getConfig().getHttpClient();

        RequestBuilder builder = new RequestBuilder().setHeader("Content-Type", "application/json; charset=utf-8")
                                                     .setBodyEncoding("UTF-8");

        if (db.getConfig().getUser() != null && db.getConfig().getPassword() != null) {
            Realm realm = new Realm.RealmBuilder()
                                   .setPrincipal(db.getConfig().getUser())
                                   .setPassword(db.getConfig().getPassword())
                                   .setUsePreemptiveAuth(true)
                                   .setScheme(AuthScheme.BASIC)
                                   .build();

            builder.setRealm(realm);
        }

        prototype = builder.build();
    }

    public CouchDb getDb() {
        return db;
    }

    public CouchDbEventListener<D> addEventHandler(CouchDbEventHandler<D> eventHandler) {
        handlers.add(eventHandler);

        return this;
    }

    public CouchDbEventListener<D> removeEventHandler(CouchDbEventHandler<D> eventHandler) {
        handlers.remove(eventHandler);

        return this;
    }

    public Future<Void> startListening() {
        return startListening(0, null);
    }

    public Future<Void> startListening(long seq) {
        return startListening(seq, null);
    }

    public Future<Void> startListening(CouchDbFilter filter) {
        return startListening(0, filter);
    }

    synchronized public Future<Void> startListening(long seq, CouchDbFilter filter) {
        if (asyncHandler == null) {
            JavaType docType = TypeFactory.defaultInstance().findTypeParameters(getClass(), CouchDbEventListener.class)[0];

            JavaType eventType = TypeFactory.defaultInstance().constructParametrizedType(CouchDbEvent.class, CouchDbEvent.class, docType);

            UrlBuilder urlBuilder = new UrlBuilder(db.getDbUrl()).addPathSegment("_changes")
                                                                 .addQueryParam("feed", "continuous")
                                                                 .addQueryParam("since", seq + "")
                                                                 .addQueryParam("heartbeat", config.getHeartbeatInMillis() + "")
                                                                 .addQueryParam("include_docs", config.isIncludeDocs() + "");
            
            if (filter != null) {
                urlBuilder.addQueryParam("filter", filter.getDesignName() + "/" + filter.getFilterName());
            }
            
            String url = urlBuilder.build();

            messagingFuture = httpClient.prepareRequest(prototype)
                                        .setUrl(url)
                                        .execute(new CouchDbEventAsyncHandler<>(handlers, mapper, eventType, url));

            return messagingFuture;
        }

        throw new IllegalStateException("Already connected!");
    }

    synchronized public void stopListening() {
        if (asyncHandler != null) {
            try {
                messagingFuture.cancel(true);
            } finally {
                asyncHandler = null;
            }
        }
    }

    @Override
    public void close() throws Exception {
        stopListening();
    }
}
