package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsUnauthorizedException extends YnsResponseException {
    public YnsUnauthorizedException(YnsHttpResponse response) {
        super(response, 401);
    }
}
