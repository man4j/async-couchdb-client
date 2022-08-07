package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsBadRequestException extends YnsResponseException {
    public YnsBadRequestException(YnsHttpResponse response) {
        super(response, 400);
    }
}
