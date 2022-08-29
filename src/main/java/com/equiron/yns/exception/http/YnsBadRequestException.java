package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsBadRequestException extends YnsResponseException {
    public YnsBadRequestException(YnsHttpResponse response) {
        super(response, 400);
    }
}
