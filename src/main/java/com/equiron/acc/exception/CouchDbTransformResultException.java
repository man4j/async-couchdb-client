package com.equiron.acc.exception;

import com.equiron.acc.CouchDbHttpResponse;

public class CouchDbTransformResultException extends CouchDbResponseException {
    public CouchDbTransformResultException(CouchDbHttpResponse response, Throwable cause) {
        super(response, cause);
    }
}
