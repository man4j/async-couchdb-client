package com.equiron.acc.view;

public interface CouchDbView {
    void update();
    
    String getDesignName();
    
    String getViewName();
}
