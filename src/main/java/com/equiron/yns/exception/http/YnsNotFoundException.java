package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsNotFoundException extends YnsResponseException {
    public YnsNotFoundException(YnsHttpResponse response) {
        super(response, 404);
    }
}
