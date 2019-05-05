package com.equiron.acc.exception;

import com.equiron.acc.CouchDbHttpResponse;

public class CouchDbUnmarshallException extends CouchDbResponseException {
    public CouchDbUnmarshallException(CouchDbHttpResponse response, Throwable cause) {
        super(response, cause);
    }
}
