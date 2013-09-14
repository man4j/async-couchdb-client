package com.n1global.acc.exception;

import com.n1global.acc.CouchDbHttpResponse;

public class CouchDbTransformResultException extends CouchDbResponseException {
    public CouchDbTransformResultException(CouchDbHttpResponse response, Throwable cause) {
        super(response, cause);
    }
}
