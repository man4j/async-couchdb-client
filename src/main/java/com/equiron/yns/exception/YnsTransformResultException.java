package com.equiron.yns.exception;

import com.equiron.yns.YnsHttpResponse;

import lombok.Getter;

@Getter
public class YnsTransformResultException extends RuntimeException {
    private YnsHttpResponse response;
    
    public YnsTransformResultException(Throwable t, YnsHttpResponse response) {
        super(t);
        this.response = response;
    }
}
