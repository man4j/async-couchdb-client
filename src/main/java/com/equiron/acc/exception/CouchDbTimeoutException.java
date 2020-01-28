package com.equiron.acc.exception;

public class CouchDbTimeoutException extends RuntimeException {
    public CouchDbTimeoutException(String message) {
        super(message);
    }
}
