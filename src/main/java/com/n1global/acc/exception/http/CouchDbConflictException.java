package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbConflictException extends CouchDbResponseException {
    public CouchDbConflictException(CouchDbHttpResponse response) {
        super(response);
    }
}
