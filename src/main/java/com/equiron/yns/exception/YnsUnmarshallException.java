package com.equiron.yns.exception;

import com.equiron.yns.YnsHttpResponse;

import lombok.Getter;

@Getter
public class YnsUnmarshallException extends RuntimeException {
    private YnsHttpResponse response;
    
    public YnsUnmarshallException(Throwable t, YnsHttpResponse response) {
        super(t);
        this.response = response;
    }
}
