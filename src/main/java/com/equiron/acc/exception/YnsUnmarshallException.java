package com.equiron.acc.exception;

import com.equiron.acc.YnsHttpResponse;

public class YnsUnmarshallException extends YnsResponseException {
    public YnsUnmarshallException(YnsHttpResponse response, Throwable cause) {
        super(response, cause, -1);
    }
}
