package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbExpectationFailedException extends CouchDbResponseException {
    public CouchDbExpectationFailedException(CouchDbHttpResponse response) {
        super(response);
    }
}
