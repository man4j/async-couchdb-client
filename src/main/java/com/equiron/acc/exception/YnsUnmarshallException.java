package com.equiron.acc.exception;

import com.equiron.acc.YnsHttpResponse;

import lombok.Getter;

@Getter
public class YnsUnmarshallException extends RuntimeException {
    private YnsHttpResponse response;
    
    public YnsUnmarshallException(Throwable t, YnsHttpResponse response) {
        super(t);
        this.response = response;
    }
}
