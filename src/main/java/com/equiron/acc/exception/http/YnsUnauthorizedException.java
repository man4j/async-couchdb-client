package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsUnauthorizedException extends YnsResponseException {
    public YnsUnauthorizedException(YnsHttpResponse response) {
        super(response, 401);
    }
}
