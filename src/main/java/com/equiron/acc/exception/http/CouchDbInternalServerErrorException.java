package com.equiron.acc.exception.http;

import com.equiron.acc.CouchDbHttpResponse;
import com.equiron.acc.exception.CouchDbResponseException;

public class CouchDbInternalServerErrorException extends CouchDbResponseException {
    public CouchDbInternalServerErrorException(CouchDbHttpResponse response) {
        super(response);
    }
}