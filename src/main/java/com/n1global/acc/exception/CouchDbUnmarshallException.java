package com.n1global.acc.exception;

import com.n1global.acc.CouchDbHttpResponse;

public class CouchDbUnmarshallException extends CouchDbResponseException {
    public CouchDbUnmarshallException(CouchDbHttpResponse response, Throwable cause) {
        super(response, cause);
    }
}
