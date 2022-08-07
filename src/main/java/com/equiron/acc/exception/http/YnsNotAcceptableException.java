package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsNotAcceptableException extends YnsResponseException {
    public YnsNotAcceptableException(YnsHttpResponse response) {
        super(response, 406);
    }
}