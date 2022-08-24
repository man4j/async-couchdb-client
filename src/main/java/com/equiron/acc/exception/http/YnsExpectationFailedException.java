package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsExpectationFailedException extends YnsResponseException {
    public YnsExpectationFailedException(YnsHttpResponse response) {
        super(response, 417);
    }
}
