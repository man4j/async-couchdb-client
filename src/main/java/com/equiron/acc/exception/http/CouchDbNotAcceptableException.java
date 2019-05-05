package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbNotAcceptableException extends CouchDbResponseException {
    public CouchDbNotAcceptableException(CouchDbHttpResponse response) {
        super(response);
    }
}