package com.equiron.acc.exception;

import com.equiron.acc.YnsHttpResponse;

import lombok.Getter;

public class YnsResponseException extends RuntimeException {
    @Getter
    private YnsHttpResponse response;
    
    @Getter
    private int status;
    
    public YnsResponseException(String message, int status) {
        super(message);
        this.status = status;
    }

    public YnsResponseException(YnsHttpResponse response, int status) {
        super(response.toString() + "\n" + response.getResponseBody());

        this.response = response;
        this.status = status;
    }

    public YnsResponseException(YnsHttpResponse response, Throwable cause, int status) {
        super(response.toString() + "\n" + response.getResponseBody(), cause);

        this.response = response;
        this.status = status;
    }
}
