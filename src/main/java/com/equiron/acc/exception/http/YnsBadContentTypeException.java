package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsBadContentTypeException extends YnsResponseException {
    public YnsBadContentTypeException(YnsHttpResponse response) {
        super(response, 415);
    }
}