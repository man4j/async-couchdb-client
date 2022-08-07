package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsExpectationFailedException extends YnsResponseException {
    public YnsExpectationFailedException(YnsHttpResponse response) {
        super(response, 417);
    }
}
