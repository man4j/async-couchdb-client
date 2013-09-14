package com.n1global.acc.notification.document;

import java.io.Closeable;

import com.n1global.acc.json.CouchDbDocument;

public interface CouchDbDocumentUpdateHandler<T extends CouchDbDocument> extends Closeable {
	void onUpdate(T doc) throws Exception;

	void onDelete(String docId) throws Exception;
}
