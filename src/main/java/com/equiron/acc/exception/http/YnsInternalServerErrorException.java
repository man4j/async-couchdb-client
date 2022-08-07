package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsInternalServerErrorException extends YnsResponseException {
    public YnsInternalServerErrorException(YnsHttpResponse response) {
        super(response, 500);
    }

    public YnsInternalServerErrorException(String error) {
        super(error, 500);
    }
}