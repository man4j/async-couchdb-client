package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbNotAllowedException extends CouchDbResponseException {
    public CouchDbNotAllowedException(CouchDbHttpResponse response) {
        super(response, 405);
    }
}