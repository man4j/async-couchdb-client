package com.n1global.acc.notification.document;

import java.io.Closeable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.n1global.acc.CouchDb;
import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbEvent;
import com.n1global.acc.notification.CouchDbEventHandler;
import com.n1global.acc.notification.CouchDbEventListener;
import com.n1global.acc.notification.CouchDbNotificationConfig;

public class CouchDbDocumentListener implements AutoCloseable {
	private CouchDbEventListener<CouchDbDocument> eventListener;

	private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	private CopyOnWriteArrayList<CouchDbDocumentUpdateHandler<CouchDbDocument>> handlers = new CopyOnWriteArrayList<>();

	private long lastSuccessSeq;

	private CouchDb db;

	private Class<? extends CouchDbDocument> docClass;

	private CouchDbDocumentListenerConfig config;

	public CouchDbDocumentListener(CouchDb db, Class<? extends CouchDbDocument> docClass, CouchDbDocumentListenerConfig config) {
	    this.db = db;
	    this.docClass = docClass;
	    this.config = config;
    }

	@SuppressWarnings("unchecked")
    public void addDocumentUpdateHandler(CouchDbDocumentUpdateHandler<? extends CouchDbDocument> handler) {
		handlers.add((CouchDbDocumentUpdateHandler<CouchDbDocument>) handler);
	}

	public void removeDocumentUpdateHandler(CouchDbDocumentUpdateHandler<? extends CouchDbDocument> handler) {
	    handlers.remove(handler);
	}

	synchronized public void startListening() throws Exception {
	    if (eventListener == null) {
    		eventListener = new CouchDbEventListener<CouchDbDocument>(db, new CouchDbNotificationConfig.Builder().setIncludeDocs(true)
                                                                                                                 .setHeartbeatInMillis(config.getHeartbeatInMillis())
                                                                                                                 .setHttpClient(config.getHttpClient())
                                                                                                                 .build()) {/*empty*/};

    		final long lastSeq = db.getInfo().getUpdateSeq();

            final CountDownLatch countDownLatch = new CountDownLatch(db.getInfo().getDocCount() == 0 ? 0 : 1);

            eventListener.addEventHandler(new CouchDbEventHandler<CouchDbDocument>() {
    			@Override
                public void onEvent(CouchDbEvent<CouchDbDocument> event) {
                    try {
                        if (event.isDeleted()) {
                        	for (CouchDbDocumentUpdateHandler<CouchDbDocument> handler : handlers) {
                        		handler.onDelete(event.getDocId());
                        	}
                        } else {
                            if (docClass.isInstance(event.getDoc())) {//Для получения правильного event.getSeq() мы должны прокрутить все документы, а не только тип docClass
                            	for (CouchDbDocumentUpdateHandler<CouchDbDocument> handler : handlers) {
                            		handler.onUpdate(event.getDoc());
                            	}
                            }
                        }

                        if (event.getSeq() >= lastSeq && countDownLatch.getCount() > 0) {//в связи с багами в http клиенте, нельзя из колбека сделать другой запрос на тот-же айпишник. этот запрос просто зависнет. по крайней мере это справедливо для ответов с заголовком "connection: close".
                            countDownLatch.countDown();
                        }

                        lastSuccessSeq = event.getSeq();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    logger.error("", e);

                    eventListener.stopListening();

                    try {
                        Thread.sleep(config.getReConnectTimeout());
                    } catch (InterruptedException e1) {
                        //ignore
                    }

                    eventListener.startListening(lastSuccessSeq);
                }
            });

            logger.info("Sequence compensation started for: " + docClass.getSimpleName());

            eventListener.startListening();

            countDownLatch.await();

            logger.info("Sequence compensation completed for: " + docClass.getSimpleName());
	    }
	}

	synchronized public void stopListening() {
	    if (eventListener != null) {
    	    try {
    	        eventListener.stopListening();
    	    } finally {
    	        eventListener = null;
    	    }
	    }
	}

	@Override
	public void close() throws Exception {
	    stopListening();

	    for (Closeable c : handlers) {
	        try {
	            c.close();
	        } catch (Exception e) {
	            logger.error("", e);
	        }
	    }
	}
}
