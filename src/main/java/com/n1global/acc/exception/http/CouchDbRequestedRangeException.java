package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbRequestedRangeException extends CouchDbResponseException {
    public CouchDbRequestedRangeException(CouchDbHttpResponse response) {
        super(response);
    }
}
