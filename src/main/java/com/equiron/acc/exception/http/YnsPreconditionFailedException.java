package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsPreconditionFailedException extends YnsResponseException {
    public YnsPreconditionFailedException(YnsHttpResponse response) {
        super(response, 412);
    }
}