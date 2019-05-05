package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbRequestedRangeException extends CouchDbResponseException {
    public CouchDbRequestedRangeException(CouchDbHttpResponse response) {
        super(response);
    }
}
