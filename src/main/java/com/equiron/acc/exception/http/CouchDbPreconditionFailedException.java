package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbPreconditionFailedException extends CouchDbResponseException {
    public CouchDbPreconditionFailedException(CouchDbHttpResponse response) {
        super(response, 412);
    }
}