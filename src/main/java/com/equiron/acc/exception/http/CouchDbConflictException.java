package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbConflictException extends CouchDbResponseException {
    public CouchDbConflictException(String message) {
        super(message);
    }

    public CouchDbConflictException(CouchDbHttpResponse response) {
        super(response);
    }
}