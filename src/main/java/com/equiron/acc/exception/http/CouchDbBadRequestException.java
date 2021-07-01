package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbBadRequestException extends CouchDbResponseException {
    public CouchDbBadRequestException(CouchDbHttpResponse response) {
        super(response, 400);
    }
}
