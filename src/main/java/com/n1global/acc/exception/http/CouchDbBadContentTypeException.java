package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbBadContentTypeException extends CouchDbResponseException {
    public CouchDbBadContentTypeException(CouchDbHttpResponse response) {
        super(response);
    }
}