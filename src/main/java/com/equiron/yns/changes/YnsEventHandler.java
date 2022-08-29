package com.equiron.yns.changes;

import com.equiron.yns.json.YnsEvent;

@FunctionalInterface
public interface YnsEventHandler {
    default void onStart() throws Exception {
        //empty
    }
    
    void onEvent(YnsEvent event) throws Exception;

    default void onError(@SuppressWarnings("unused") Throwable e) throws Exception {
        //empty
    }
    
    default void onCancel() throws Exception {
        //empty
    }
}
