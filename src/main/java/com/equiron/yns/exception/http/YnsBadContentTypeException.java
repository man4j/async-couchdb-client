package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsBadContentTypeException extends YnsResponseException {
    public YnsBadContentTypeException(YnsHttpResponse response) {
        super(response, 415);
    }
}