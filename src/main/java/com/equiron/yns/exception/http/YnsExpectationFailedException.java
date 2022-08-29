package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsExpectationFailedException extends YnsResponseException {
    public YnsExpectationFailedException(YnsHttpResponse response) {
        super(response, 417);
    }
}
