package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsInternalServerErrorException extends YnsResponseException {
    public YnsInternalServerErrorException(YnsHttpResponse response) {
        super(response, 500);
    }

    public YnsInternalServerErrorException(String error) {
        super(error, 500);
    }
}