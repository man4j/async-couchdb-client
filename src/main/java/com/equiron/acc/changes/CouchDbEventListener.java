package com.equiron.acc.changes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equiron.acc.CouchDb;
import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;
import com.equiron.acc.util.BufUtils;
import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public abstract class CouchDbEventListener<D extends CouchDbDocument> implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    
    private final CouchDb db;

    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private volatile Future<Void> messagingFuture = null;
    
    private volatile String lastSuccessSeq;
    
    private volatile byte[] eventBuf = new byte[0];
    
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
                Builder builder = db.getRequestPrototype();
                
                if (selector == null) {
                    builder.GET();
                } else {
                    builder.POST(BodyPublishers.ofString(new ObjectMapper().writeValueAsString(Collections.singletonMap("selector", selector))));
                }
                
                messagingFuture = db.getHttpClient().sendAsync(builder.uri(URI.create(url)).timeout(Duration.ofDays(Integer.MAX_VALUE)).build(), BodyHandlers.ofInputStream()).thenAccept(wrapper -> {
                    if (wrapper.statusCode() == 200) {
                        logger.debug("Start listening " + url);
                        
                        for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                            try {
                                eventHandler.onStart();
                            } catch (Exception e) {
                                logger.error("Error in " + url, e);
                            }
                        }
                    } else {
                        throw new RuntimeException("Can't connect to server. Status code: " + wrapper.statusCode() + "");
                    }
                    
                    try (InputStream in = wrapper.body()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead = in.read(buffer);
    
                        while (bytesRead != -1) {
                            eventBuf = BufUtils.concat(buffer, eventBuf, bytesRead);

                            int startPos = 0;

                            for (int curPos = (eventBuf.length - bytesRead); curPos < eventBuf.length; curPos++) {
                                if (eventBuf[curPos] == '\n') {
                                    if (curPos == 0 || eventBuf[curPos - 1] == '\n') {
                                        logger.debug("Received heartbeat from " + url);
                                    } else {
                                        int eventLength = curPos - startPos;

                                        byte[] eventArray = new byte[eventLength];

                                        System.arraycopy(eventBuf, startPos, eventArray, 0, eventLength);

                                        processEvent(eventArray, eventType);
                                    }

                                    startPos = curPos + 1;
                                }
                            }

                            if (startPos > 0) {
                                eventBuf = Arrays.copyOfRange(eventBuf, startPos, eventBuf.length);
                            }
    
                            bytesRead = in.read(buffer);
                        }
                        
                        if (bytesRead == -1) {
                            if (!isStopped()) {//не было остановки, просто клиент решил сам что соединение закрылось
                                throw new IOException("Unexpected listener stop (Connection closed by proxy?)");
                            }
                            
                            for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                                try {
                                    eventHandler.onCancel();
                                } catch (Exception e) {
                                    logger.error("Error in " + url, e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing event " + url, e);

                        for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                            try {
                                eventHandler.onError(e);
                            } catch (Exception ex) {
                                logger.error("Error in " + url, ex);
                            }
                        }

                        restart();
                    }
                });
                
                logger.info("CouchDB listener started: " + url);
    
                return messagingFuture;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
    
    private void processEvent(byte[] eventArray, JavaType eventType) throws Exception {
        for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
            JsonNode node = db.getMapper().readTree(eventArray);
            
            CouchDbEvent<D> event;
            
            if (node.path("deleted").asBoolean()) {
                event = new CouchDbEvent<>(node.path("id").asText(), node.path("seq").asText(), true);
                eventHandler.onEvent(event);
            } else {
                event = db.getMapper().readValue(eventArray, new TypeReference<CouchDbEvent<D>>() { /* empty */});

                if (eventType.containedType(0).getRawClass().isInstance(event.getDoc())) {
                    eventHandler.onEvent(event);
                }
            }
            
            lastSuccessSeq = event.getSeq();
        }
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
    
    private void restart() {
        try {
            stopListening();
            
            Thread.sleep(5_000);
        } catch (Exception e) {
            logger.error("", e);
        }
        
        startListening(lastSuccessSeq);
    }
}
