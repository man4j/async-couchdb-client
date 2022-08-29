package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsNotAllowedException extends YnsResponseException {
    public YnsNotAllowedException(YnsHttpResponse response) {
        super(response, 405);
    }
}