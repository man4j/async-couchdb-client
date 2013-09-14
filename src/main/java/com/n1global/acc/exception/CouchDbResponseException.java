package com.n1global.acc.exception;

import com.n1global.acc.CouchDbHttpResponse;

public class CouchDbResponseException extends RuntimeException {
    private CouchDbHttpResponse response;

    public CouchDbResponseException(CouchDbHttpResponse response) {
        super(response.toString());

        this.response = response;
    }

    public CouchDbResponseException(CouchDbHttpResponse response, Throwable cause) {
        super(response.toString(), cause);

        this.response = response;
    }

    public CouchDbHttpResponse getResponse() {
        return response;
    }
}
