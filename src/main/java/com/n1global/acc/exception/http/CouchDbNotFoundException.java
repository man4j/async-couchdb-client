package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbNotFoundException extends CouchDbResponseException {
    public CouchDbNotFoundException(CouchDbHttpResponse response) {
        super(response);
    }
}
