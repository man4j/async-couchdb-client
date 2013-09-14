package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbNotAllowedException extends CouchDbResponseException {
    public CouchDbNotAllowedException(CouchDbHttpResponse response) {
        super(response);
    }
}