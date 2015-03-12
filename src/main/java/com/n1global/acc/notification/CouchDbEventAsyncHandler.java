package com.n1global.acc.notification;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbEvent;
import com.n1global.acc.util.BufUtils;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class CouchDbEventAsyncHandler<D extends CouchDbDocument> implements AsyncHandler<Void> {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    private byte[] eventBuf = new byte[0];

    private CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers = new CopyOnWriteArrayList<>();

    private ObjectMapper mapper;

    private JavaType eventType;
    
    private String url;

    public CouchDbEventAsyncHandler(CopyOnWriteArrayList<CouchDbEventHandler<D>> handlers, ObjectMapper mapper, JavaType eventType, String url) {
        this.handlers = handlers;
        this.mapper = mapper;
        this.eventType = eventType;
        this.url = url;
    }

    @Override
    public void onThrowable(Throwable t) {
        if (!(t instanceof CancellationException)) {
            for (CouchDbEventHandler<D> eventHandler : handlers) {
                try {
                    eventHandler.onError(t);
                } catch (Exception e) {
                    logger.error("Error in " + url, e);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        byte[] body = bodyPart.getBodyPartBytes();

        eventBuf = BufUtils.concat(body, eventBuf);

        int lastPos = 0;

        for (int i = 0; i < eventBuf.length; i++) {
            if (eventBuf[i] == '\n') {
                if (i == 0 || eventBuf[i - 1] == '\n') {
                    logger.debug("Received heartbeat from " + url);
                } else {
                    int eventLength = i - lastPos;

                    byte[] eventArray = new byte[eventLength];

                    System.arraycopy(eventBuf, lastPos, eventArray, 0, eventLength);

                    for (CouchDbEventHandler<D> eventHandler : handlers) {
                        try {
                            JsonNode node = mapper.readTree(eventArray);

                            if (node.path("deleted").asBoolean()) {
                                eventHandler.onEvent(new CouchDbEvent(node.path("id").asText(), node.path("seq").asLong(), true));
                            } else {
                                CouchDbEvent<CouchDbDocument> event = mapper.readValue(eventArray, new TypeReference<CouchDbEvent<CouchDbDocument>>() { /* empty */});

                                if (eventType.containedType(0).getRawClass().isInstance(event.getDoc())) {
                                    eventHandler.onEvent((CouchDbEvent<D>) event);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error in " + url, e);
                        }
                    }
                }

                lastPos = i + 1;
            }
        }

        eventBuf = Arrays.copyOfRange(eventBuf, lastPos, eventBuf.length);

        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        if (responseStatus.getStatusCode() == 200) {
            logger.debug("Start listening " + url);

            return AsyncHandler.STATE.CONTINUE;
        }

        throw new RuntimeException(responseStatus.getStatusCode() + " / " + responseStatus.getStatusText());
    }

    @Override
    public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public Void onCompleted() throws Exception {
        logger.debug("Stop listening " + url);

        return null;
    }
}
