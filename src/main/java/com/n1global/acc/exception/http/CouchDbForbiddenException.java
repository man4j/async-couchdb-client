package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbForbiddenException extends CouchDbResponseException {
    public CouchDbForbiddenException(CouchDbHttpResponse response) {
        super(response);
    }
}
