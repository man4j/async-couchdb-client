package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbNotFoundException extends CouchDbResponseException {
    public CouchDbNotFoundException(CouchDbHttpResponse response) {
        super(response, 404);
    }
}
