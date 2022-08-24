package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsBadRequestException extends YnsResponseException {
    public YnsBadRequestException(YnsHttpResponse response) {
        super(response, 400);
    }
}
