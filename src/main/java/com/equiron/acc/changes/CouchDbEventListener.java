package com.equiron.acc.changes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
import com.rainerhahnekamp.sneakythrow.Sneaky;

public abstract class CouchDbEventListener<D extends CouchDbDocument> implements AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    
    private final CouchDb db;

    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private volatile String lastSuccessSeq;
    
    private volatile Thread listenerThread = null;
    
    private volatile boolean interrupted;
    
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
    
    public synchronized void startListening(String seq) {
        startListening(seq, null);
    }

    public synchronized void startListening(String seq, Map<String, Object> selector) {
        if (listenerThread == null) {
            lastSuccessSeq = seq;
            
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            
            JavaType docType = typeFactory.findTypeParameters(typeFactory.constructType(getClass()), CouchDbEventListener.class)[0];
            
            JavaType eventType = typeFactory.constructParametricType(CouchDbEvent.class, docType);

            interrupted = false;

            listenerThread = new Thread() {
                @Override
                public void run() {
                    while (!interrupted) {
                        UrlBuilder urlBuilder = new UrlBuilder(db.getDbUrl()).addPathSegment("_changes")
                                                                             .addQueryParam("feed", "continuous")
                                                                             .addQueryParam("heartbeat", "15000")
                                                                             .addQueryParam("since", lastSuccessSeq)
                                                                             .addQueryParam("include_docs", Boolean.toString(true));
                        Builder builder = db.getRequestPrototype();
                        
                        if (selector == null) {
                            builder.GET();
                        } else {
                            urlBuilder.addQueryParam("filter", "_selector");
                            builder.POST(BodyPublishers.ofString(Sneaky.sneak(() -> new ObjectMapper().writeValueAsString(Collections.singletonMap("selector", selector)))));
                        }
                        
                        String url = urlBuilder.build();
                        
                        try {
                            HttpResponse<InputStream> response = db.getHttpClient().send(builder.uri(URI.create(url)).timeout(Duration.ofDays(Integer.MAX_VALUE)).build(), BodyHandlers.ofInputStream());
                            
                            if (response.statusCode() == 200) {
                                logger.debug("Start listening " + url);
                                
                                for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                                    try {
                                        eventHandler.onStart();
                                    } catch (Exception e) {
                                        logger.error("Error in " + url, e);
                                    }
                                }
                            } else {
                                throw new IOException("Can't connect to server. Status code: " + response.statusCode() + "");
                            }
                            
                            try (InputStream in = response.body()) {
                                byte[] buffer = new byte[8192];
                                int bytesRead = in.read(buffer);
                                
                                byte[] eventBuf = new byte[0];
            
                                while (bytesRead != -1 && !interrupted) {
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
                            }
                        } catch (@SuppressWarnings("unused") InterruptedException e) {
                            logger.info("CouchDB listener interrupted: " + url);
                            interrupted = true;
                        } catch (Exception e) {
                            logger.error("Error processing event " + url, e);
    
                            for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                                try {
                                    eventHandler.onError(e);
                                } catch (Exception ex) {
                                    logger.error("Error in " + url, ex);
                                }
                            }
                            
                            try {
                                Thread.sleep(5_000);
                            } catch (@SuppressWarnings("unused") InterruptedException ex) {
                                logger.info("CouchDB listener interrupted: " + url);
                                interrupted = true;
                            }
                        }
                    }
                }
            };
            
            listenerThread.setName("CouchDB listener for: " + db.getDbName());
            listenerThread.start();
        }
    }
    
    private void processEvent(byte[] eventArray, JavaType eventType) throws Exception {
        JsonNode node = db.getMapper().readTree(eventArray);
        
        CouchDbEvent<D> event;
        boolean processEvent = true;
        
        if (node.path("deleted").asBoolean()) {
            event = new CouchDbEvent<>(node.path("id").asText(), node.path("seq").asText(), true);                
        } else {
            event = db.getMapper().readValue(eventArray, new TypeReference<CouchDbEvent<D>>() { /* empty */});
            
            if (!eventType.containedType(0).getRawClass().isInstance(event.getDoc())) {
                processEvent = false;
            }
        }
        
        if (processEvent) {
            for (CouchDbEventHandler<D> eventHandler : getHandlers()) {
                eventHandler.onEvent(event);
            }
        }
        
        lastSuccessSeq = event.getSeq();
    }
    
    public boolean isStopped() {
        return listenerThread == null;
    }

    public synchronized void stopListening() {
        if (listenerThread != null) {
            try {
                interrupted = true;
                
                listenerThread.join();
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                //empty
            } finally {
                listenerThread = null;
            }
            
            logger.info("CouchDB listener stopped: " + db.getDbUrl());
        }
    }

    @Override
    public void close() throws Exception {
        stopListening();
    }
}
