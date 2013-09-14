package com.n1global.acc.exception.http;

import com.n1global.acc.CouchDbHttpResponse;
import com.n1global.acc.exception.CouchDbResponseException;

public class CouchDbBadRequestException extends CouchDbResponseException {
    public CouchDbBadRequestException(CouchDbHttpResponse response) {
        super(response);
    }
}
