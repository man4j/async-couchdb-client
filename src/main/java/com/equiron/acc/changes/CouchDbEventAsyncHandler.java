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

    public CouchDbEventAsyncHandler(CouchDbEventListener<D> eventListener, ObjectMapper mapper, JavaType eventType, String url) {
        this.eventListener = eventListener;
        this.mapper = mapper;
        this.eventType = eventType;
        this.url = url;
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
            for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
                try {
                    eventHandler.onError(t);
                } catch (Exception e) {
                    logger.error("Error in " + url, e);
                }
            }
            
            eventListener.stopListening();

            try {
                Thread.sleep(5_000);
            } catch (@SuppressWarnings("unused") InterruptedException e1) {
                //ignore
            }

            eventListener.startListening(lastSuccessSeq);
        }
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

    @SuppressWarnings("unchecked")
    private void processEvent(byte[] eventArray) {
        for (CouchDbEventHandler<D> eventHandler : eventListener.getHandlers()) {
            try {                
                CouchDbEvent<CouchDbDocument> event = mapper.readValue(eventArray, new TypeReference<CouchDbEvent<CouchDbDocument>>() { /* empty */});

                if (eventType.containedType(0).getRawClass().isInstance(event.getDoc())) {
                    eventHandler.onEvent((CouchDbEvent<D>) event);
                }
                
                lastSuccessSeq = event.getSeq();
            } catch (Exception e) {
                logger.error("Error in " + url, e);
            }
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
    public State onHeadersReceived(HttpHeaders headers) throws Exception {
        return State.CONTINUE;
    }

    @Override
    public Void onCompleted() throws Exception {
        logger.debug("Stop listening " + url);
        
        return null;
    }
}
