package com.equiron.acc.changes;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;
import com.equiron.acc.util.BufUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpHeaders;

public class CouchDbEventAsyncHandler<D extends CouchDbDocument> implements AsyncHandler<Void> {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private volatile byte[] eventBuf = new byte[0];

    private final CouchDbEventListener<D> eventListener;

    private final ObjectMapper mapper;

    private final JavaType eventType;
    
    private final String url;
    
    private volatile String lastSuccessSeq;

    public CouchDbEventAsyncHandler(CouchDbEventListener<D> eventListener, ObjectMapper mapper, JavaType eventType, String url, String lastSuccessSeq) {
        this.eventListener = eventListener;
        this.mapper = mapper;
        this.eventType = eventType;
        this.url = url;
        this.lastSuccessSeq = lastSuccessSeq;
    }

    @Override
    public void onThrowable(Throwable t) {
        if (t instanceof CancellationException) {
            for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
                try {
                    eventHandler.onCancel();
                } catch (Exception e) {
                    logger.error("Error in " + url, e);
                }
            }
        } else {
            logger.error("Error processing event " + url, t);

            for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
                try {
                    eventHandler.onError(t);
                } catch (Exception e) {
                    logger.error("Error in " + url, e);
                }
            }

            restart();
        }
    }
    
    @Override
    public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        if (responseStatus.getStatusCode() == 200) {
            logger.debug("Start listening " + url);
            
            for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
                try {
                    eventHandler.onStart();
                } catch (Exception e) {
                    logger.error("Error in " + url, e);
                }
            }

            return State.CONTINUE;
        }

        throw new RuntimeException(responseStatus.getStatusCode() + " / " + responseStatus.getStatusText());
    }

    @Override
    public Void onCompleted() throws Exception {
        logger.info("Stop listening " + url);
        
        if (!eventListener.isStopped()) {//не было остановки, просто клиент решил сам что соединение закрылось
            throw new RuntimeException("Unexpected listener stop (Connection closed by proxy?)");
        }
        
        return null;
    }
    
    private void restart() {
        try {
            eventListener.stopListening();
            
            Thread.sleep(5_000);
        } catch (Exception e) {
            logger.error("", e);
        }
        
        eventListener.startListening(lastSuccessSeq);
    }

    @Override
    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        byte[] body = bodyPart.getBodyPartBytes();

        eventBuf = BufUtils.concat(body, eventBuf);

        int startPos = 0;

        for (int curPos = (eventBuf.length - body.length); curPos < eventBuf.length; curPos++) {
            if (eventBuf[curPos] == '\n') {
                if (curPos == 0 || eventBuf[curPos - 1] == '\n') {
                    logger.debug("Received heartbeat from " + url);
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

        return State.CONTINUE;
    }

    private void processEvent(byte[] eventArray) throws Exception {
        for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
            JsonNode node = mapper.readTree(eventArray);
            
            CouchDbEvent<D> event;
            
            if (node.path("deleted").asBoolean()) {
                event = new CouchDbEvent<>(node.path("id").asText(), node.path("seq").asText(), true);
                eventHandler.onEvent(event);
            } else {
                event = mapper.readValue(eventArray, new TypeReference<CouchDbEvent<D>>() { /* empty */});

                if (eventType.containedType(0).getRawClass().isInstance(event.getDoc())) {
                    eventHandler.onEvent(event);
                }
            }
            
            lastSuccessSeq = event.getSeq();
        }
    }

    @Override
    public State onHeadersReceived(HttpHeaders headers) throws Exception {
        return State.CONTINUE;
    }
}
