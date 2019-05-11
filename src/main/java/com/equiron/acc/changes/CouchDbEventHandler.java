package com.equiron.acc.changes;

import com.equiron.acc.json.CouchDbDocument;
import com.equiron.acc.json.CouchDbEvent;

@FunctionalInterface
public interface CouchDbEventHandler<D extends CouchDbDocument> {
    default void onStart() throws Exception {
        //empty
    }
    
    void onEvent(CouchDbEvent<D> event) throws Exception;

    default void onError(Throwable e) throws Exception {
        //empty
    }
    
    default void onCancel() throws Exception {
        //empty
    }
}
