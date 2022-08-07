package com.equiron.acc.exception;

import com.equiron.acc.YnsHttpResponse;

public class YnsTransformResultException extends YnsResponseException {
    public YnsTransformResultException(YnsHttpResponse response, Throwable cause) {
        super(response, cause, -1);
    }
}
