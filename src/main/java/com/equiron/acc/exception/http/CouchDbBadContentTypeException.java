package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbBadContentTypeException extends CouchDbResponseException {
    public CouchDbBadContentTypeException(CouchDbHttpResponse response) {
        super(response, 415);
    }
}