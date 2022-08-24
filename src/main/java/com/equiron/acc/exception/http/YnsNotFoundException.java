package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsNotFoundException extends YnsResponseException {
    public YnsNotFoundException(YnsHttpResponse response) {
        super(response, 404);
    }
}
