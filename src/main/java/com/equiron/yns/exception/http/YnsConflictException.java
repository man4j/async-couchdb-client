package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsConflictException extends YnsResponseException {
    public YnsConflictException(String message) {
        super(message, 409);
    }

    public YnsConflictException(YnsHttpResponse response) {
        super(response, 409);
    }
}
