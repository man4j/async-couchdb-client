package com.equiron.acc.exception;

import com.equiron.acc.YnsHttpResponse;

import lombok.Getter;

@Getter
public class YnsTransformResultException extends RuntimeException {
    private YnsHttpResponse response;
    
    public YnsTransformResultException(Throwable t, YnsHttpResponse response) {
        super(t);
        this.response = response;
    }
}
