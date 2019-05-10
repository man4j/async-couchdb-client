package com.equiron.acc.changes;

import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;

public interface CouchDbEventHandler<D extends CouchDbDocument> {
    void onStart() throws Exception;
    
    void onEvent(CouchDbEvent<D> event) throws Exception;

    void onError(Throwable e) throws Exception;
    
    void onCancel() throws Exception;
}
