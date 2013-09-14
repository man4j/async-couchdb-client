package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbUnauthorizedException extends CouchDbResponseException {
    public CouchDbUnauthorizedException(CouchDbHttpResponse response) {
        super(response);
    }
}
