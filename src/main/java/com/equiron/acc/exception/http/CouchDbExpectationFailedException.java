package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbExpectationFailedException extends CouchDbResponseException {
    public CouchDbExpectationFailedException(CouchDbHttpResponse response) {
        super(response, 417);
    }
}
