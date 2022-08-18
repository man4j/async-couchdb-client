package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsNotAllowedException extends YnsResponseException {
    public YnsNotAllowedException(YnsHttpResponse response) {
        super(response, 405);
    }
}