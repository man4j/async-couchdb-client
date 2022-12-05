package com.equiron.yns.changes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import com.equiron.yns.YnsDb;
import com.equiron.yns.json.YnsEvent;
import com.equiron.yns.provider.HttpClientProviderResponse;
import com.equiron.yns.util.BufUtils;
import com.equiron.yns.util.UrlBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YnsEventListener implements AutoCloseable, YnsEventHandler {
    private volatile YnsDb db;
    
    private volatile YnsSequenceStorage sequenceStorage;

    private CopyOnWriteArrayList<YnsEventHandler> handlers = new CopyOnWriteArrayList<>();

    private volatile String lastSuccessSeq;
    
    private volatile Thread listenerThread = null;
    
    private volatile boolean interrupted;

    public YnsEventListener(YnsDb db, YnsSequenceStorage sequenceStorage) {
        this.db = db;
        this.sequenceStorage = sequenceStorage;
    }
    
    public YnsDb getDb() {
        return db;
    }

    public YnsEventListener addEventHandler(YnsEventHandler eventHandler) {
        handlers.add(eventHandler);
        return this;
    }

    public YnsEventListener removeEventHandler(YnsEventHandler eventHandler) {
        handlers.remove(eventHandler);
        return this;
    }
    
    public CopyOnWriteArrayList<YnsEventHandler> getHandlers() {
        return handlers;
    }
    
    @SuppressWarnings("resource")
    @PostConstruct
    public synchronized void startListening() {
        addEventHandler(this);
        startListening(null);
    }

    public synchronized void startListening(Map<String, Object> selector) {
        if (listenerThread == null) {
            lastSuccessSeq = sequenceStorage.readSequence();
            
            interrupted = false;

            listenerThread = new Thread() {
                @Override
                public void run() {
                    while (!interrupted) {
                        UrlBuilder urlBuilder = new UrlBuilder(db.getDbUrl()).addPathSegment("_changes")
                                                                             .addQueryParam("feed", "continuous")
                                                                             .addQueryParam("heartbeat", "15000")
                                                                             .addQueryParam("since", lastSuccessSeq);
                        
                        String method;
                        String body = "";
                        
                        if (selector == null) {
                            method = "GET";
                        } else {
                            urlBuilder.addQueryParam("filter", "_selector");
                            method = "POST";
                            body = Sneaky.sneak(() -> new ObjectMapper().writeValueAsString(Collections.singletonMap("selector", selector)));
                        }
                        
                        String url = urlBuilder.build();
                        
                        try {
                            HttpClientProviderResponse response = db.getHttpClientProvider().getStream(url, method, body, null);
                            
                            if (response.getStatus() == 200) {
                                log.debug("Start listening " + url);
                                
                                for (YnsEventHandler eventHandler : getHandlers()) {
                                    try {
                                        eventHandler.onStart();
                                    } catch (Exception e) {
                                        log.error("Error in " + url, e);
                                    }
                                }
                            } else {
                                throw new IOException("Can't connect to server. Status code: " + response.getStatus() + "");
                            }
                            
                            try (InputStream in = response.getIn()) {
                                byte[] buffer = new byte[8192];
                                int bytesRead = in.read(buffer);
                                
                                byte[] eventBuf = new byte[0];
            
                                while (bytesRead != -1 && !interrupted) {
                                    eventBuf = BufUtils.concat(buffer, eventBuf, bytesRead);
    
                                    int startPos = 0;
    
                                    for (int curPos = (eventBuf.length - bytesRead); curPos < eventBuf.length; curPos++) {
                                        if (eventBuf[curPos] == '\n') {
                                            if (curPos == 0 || eventBuf[curPos - 1] == '\n') {
                                                log.debug("Received heartbeat from " + url);
                                            } else {
                                                int eventLength = curPos - startPos;
    
                                                byte[] eventArray = new byte[eventLength];
    
                                                System.arraycopy(eventBuf, startPos, eventArray, 0, eventLength);
    
                                                processEvent(eventArray);
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
                                    
                                    for (YnsEventHandler eventHandler : getHandlers()) {
                                        try {
                                            eventHandler.onCancel();
                                        } catch (Exception e) {
                                            log.error("Error in " + url, e);
                                        }
                                    }
                                }
                            }
                        } catch (@SuppressWarnings("unused") InterruptedException e) {
                            log.info("Yns listener interrupted: " + url);
                            interrupted = true;
                        } catch (Exception e) {
                            log.error("Error processing event " + url, e);
    
                            for (YnsEventHandler eventHandler : getHandlers()) {
                                try {
                                    eventHandler.onError(e);
                                } catch (Exception ex) {
                                    log.error("Error in " + url, ex);
                                }
                            }
                            
                            try {
                                Thread.sleep(5_000);
                            } catch (@SuppressWarnings("unused") InterruptedException ex) {
                                log.info("Yns listener interrupted: " + url);
                                interrupted = true;
                            }
                        }
                    }
                }
            };
            
            listenerThread.setName("Yns listener for: " + db.getDbName());
            listenerThread.start();
        }
    }
    
    private void processEvent(byte[] eventArray) throws Exception {
        YnsEvent event = db.getMapper().readValue(eventArray, YnsEvent.class);
        
        for (YnsEventHandler eventHandler : getHandlers()) {
            eventHandler.onEvent(event);
        }
        
        sequenceStorage.saveSequence(event.getSeq());
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
            
            log.info("Yns listener stopped: " + db.getDbUrl());
        }
    }

    @Override
    public void close() throws Exception {
        stopListening();
    }
    
    @Override
    public void onStart() throws Exception {
        //empty
    }
    
    @Override
    public void onEvent(YnsEvent event) throws Exception {
        //empty
    }

    @Override
    public void onError(Throwable e) throws Exception {
        //empty
    }
    
    @Override
    public void onCancel() throws Exception {
        //empty
    }
}
