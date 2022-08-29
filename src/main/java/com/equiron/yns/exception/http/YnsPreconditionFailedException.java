package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsPreconditionFailedException extends YnsResponseException {
    public YnsPreconditionFailedException(YnsHttpResponse response) {
        super(response, 412);
    }
}