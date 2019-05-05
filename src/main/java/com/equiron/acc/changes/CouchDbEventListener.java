package com.equiron.acc.changes;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class CouchDbEventListener<D extends CouchDbDocument> implements AutoCloseable {
    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private Future<Void> messagingFuture;

    private final CouchDb db;

    public CouchDbEventListener(CouchDb db) {
        this.db = db;
    }

    public CouchDbEventListener<D> addEventHandler(CouchDbEventHandler<D> eventHandler) {
        handlers.add(eventHandler);

        return this;
    }

    public CouchDbEventListener<D> removeEventHandler(CouchDbEventHandler<D> eventHandler) {
        handlers.remove(eventHandler);

        return this;
    }
    
    public CopyOnWriteArrayList<CouchDbEventHandler<D>> getHandlers() {
        return handlers;
    }

    public Future<Void> startListening() {
        return startListening("0");
    }

    public synchronized Future<Void> startListening(String seq) {
        if (messagingFuture == null) {
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            
            JavaType docType = typeFactory.findTypeParameters(typeFactory.constructType(getClass()), CouchDbEventListener.class)[0];

            JavaType eventType = typeFactory.constructParametricType(CouchDbEvent.class, docType);

            UrlBuilder urlBuilder = new UrlBuilder(db.getDbUrl()).addPathSegment("_changes")
                                                                 .addQueryParam("feed", "continuous")
                                                                 .addQueryParam("since", seq)
                                                                 .addQueryParam("include_docs", Boolean.toString(true));
                        
            String url = urlBuilder.build();

            messagingFuture = db.getConfig().getHttpClient().prepareRequest(db.getPrototype())
                                            .setUrl(url)
                                            .execute(new CouchDbEventAsyncHandler<>(this, db.getMapper(), eventType, url));

            return messagingFuture;
        }

        throw new IllegalStateException("Already connected!");
    }

    public synchronized void stopListening() {
        if (messagingFuture != null) {
            try {
                messagingFuture.cancel(true);
            } finally {
                messagingFuture = null;
            }
        }
    }

    @Override
    public void close() throws Exception {
        stopListening();
    }
}
