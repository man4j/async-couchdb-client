package com.equiron.acc.changes;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class CouchDbEventListener<D extends CouchDbDocument> implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    
    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private volatile Future<Void> messagingFuture = null;

    private final CouchDb db;
    
    private final AsyncHttpClient client;

    public CouchDbEventListener(CouchDb db, AsyncHttpClient client) {
        this.db = db;
        this.client = client;
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
    
    public synchronized Future<Void> startListening(String seq) {
        return startListening(seq, null);
    }

    public synchronized Future<Void> startListening(String seq, Map<String, Object> selector) {
        if (messagingFuture == null) {            
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            
            JavaType docType = typeFactory.findTypeParameters(typeFactory.constructType(getClass()), CouchDbEventListener.class)[0];

            JavaType eventType = typeFactory.constructParametricType(CouchDbEvent.class, docType);

            UrlBuilder urlBuilder = new UrlBuilder(db.getDbUrl()).addPathSegment("_changes")
                                                                 .addQueryParam("feed", "continuous")
                                                                 .addQueryParam("heartbeat", "15000")
                                                                 .addQueryParam("since", seq)
                                                                 .addQueryParam("include_docs", Boolean.toString(true));
            
            if (selector != null) {
                urlBuilder.addQueryParam("filter", "_selector");
            }
            
            String url = urlBuilder.build();

            try {
                messagingFuture = client.prepareRequest(db.getPrototype())
                                        .setMethod(selector == null ? "GET" : "POST")
                                        .setBody(selector == null ? null : new ObjectMapper().writeValueAsString(Collections.singletonMap("selector", selector)))
                                        .setUrl(url)
                                        .execute(new CouchDbEventAsyncHandler<>(this, db.getMapper(), eventType, url, seq));
                
                logger.info("CouchDB listener started: " + url);
    
                return messagingFuture;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
    
    public boolean isStopped() {
        return messagingFuture == null;
    }

    public synchronized void stopListening() {
        if (messagingFuture != null) {
            try {
                messagingFuture.cancel(true);
            } finally {
                messagingFuture = null;
            }
            
            logger.info("CouchDB listener stopped: " + db.getDbUrl());
        }
    }

    @Override
    public void close() throws Exception {
        stopListening();
    }
}
