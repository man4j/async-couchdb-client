package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;

public class YnsRequestedRangeException extends YnsResponseException {
    public YnsRequestedRangeException(YnsHttpResponse response) {
        super(response, 416);
    }
}
