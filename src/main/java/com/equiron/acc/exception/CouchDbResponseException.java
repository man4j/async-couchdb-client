package com.equiron.acc.exception;

import com.equiron.acc.CouchDbHttpResponse;

public class CouchDbResponseException extends RuntimeException {
    private CouchDbHttpResponse response;
    
    public CouchDbResponseException(String message) {
        super(message);
    }

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
