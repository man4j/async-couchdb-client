package com.n1global.acc.exception;

import com.n1global.acc.CouchDbHttpResponse;

public class CouchDbResponseException extends RuntimeException {
    private CouchDbHttpResponse response;

    public CouchDbResponseException(CouchDbHttpResponse response) {
        super(response.toString() + "\n" + response.getResponseBody());

        this.response = response;
    }

    public CouchDbResponseException(CouchDbHttpResponse response, Throwable cause) {
        super(response.toString() + "\n" + response.getResponseBody(), cause);

        this.response = response;
    }

    public CouchDbHttpResponse getResponse() {
        return response;
    }
}
