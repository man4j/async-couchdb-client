package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbUnauthorizedException extends CouchDbResponseException {
    public CouchDbUnauthorizedException(CouchDbHttpResponse response) {
        super(response, 401);
    }
}
