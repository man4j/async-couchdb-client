package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbForbiddenException extends CouchDbResponseException {
    public CouchDbForbiddenException(String message) {
        super(message, 403);
    }
    
    public CouchDbForbiddenException(CouchDbHttpResponse response) {
        super(response, 403);
    }
}
