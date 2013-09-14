package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbPreconditionFailedException extends CouchDbResponseException {
    public CouchDbPreconditionFailedException(CouchDbHttpResponse response) {
        super(response);
    }
}