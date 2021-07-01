package com.equiron.acc.exception;

import com.equiron.acc.CouchDbHttpResponse;

public class CouchDbResponseException extends RuntimeException {
    private CouchDbHttpResponse response;
    
    private int status;
    
    public CouchDbResponseException(String message, int status) {
        super(message);
        this.status = status;
    }

    public CouchDbResponseException(CouchDbHttpResponse response, int status) {
        super(response.toString() + "\n" + response.getResponseBody());

        this.response = response;
        this.status = status;
    }

    public CouchDbResponseException(CouchDbHttpResponse response, Throwable cause, int status) {
        super(response.toString() + "\n" + response.getResponseBody(), cause);

        this.response = response;
        this.status = status;
    }

    public CouchDbHttpResponse getResponse() {
        return response;
    }
    
    public int getStatus() {
        return status;
    }
}
