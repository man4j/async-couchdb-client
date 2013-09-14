package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbNotAcceptableException extends CouchDbResponseException {
    public CouchDbNotAcceptableException(CouchDbHttpResponse response) {
        super(response);
    }
}