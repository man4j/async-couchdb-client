package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsUnauthorizedException extends YnsResponseException {
    public YnsUnauthorizedException(YnsHttpResponse response) {
        super(response, 401);
    }
}
