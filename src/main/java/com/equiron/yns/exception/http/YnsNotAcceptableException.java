package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsNotAcceptableException extends YnsResponseException {
    public YnsNotAcceptableException(YnsHttpResponse response) {
        super(response, 406);
    }
}