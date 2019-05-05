package com.equiron.acc.changes;

import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;

public interface CouchDbEventHandler<D extends CouchDbDocument> {
    void onEvent(CouchDbEvent<D> event);

    void onError(Throwable e);
}
