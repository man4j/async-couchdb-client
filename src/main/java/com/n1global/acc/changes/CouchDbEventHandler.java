package com.n1global.acc.changes;

import com.n1global.acc.json.CouchDbDocument;
import com.n1global.acc.json.CouchDbEvent;

public interface CouchDbEventHandler<D extends CouchDbDocument> {
    void onEvent(CouchDbEvent<D> event);

    void onError(Throwable e);
}
