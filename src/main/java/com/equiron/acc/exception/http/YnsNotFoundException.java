package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsNotFoundException extends YnsResponseException {
    public YnsNotFoundException(YnsHttpResponse response) {
        super(response, 404);
    }
}
