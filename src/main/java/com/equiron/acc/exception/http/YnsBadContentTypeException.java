package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsBadContentTypeException extends YnsResponseException {
    public YnsBadContentTypeException(YnsHttpResponse response) {
        super(response, 415);
    }
}