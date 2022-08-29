package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsRequestedRangeException extends YnsResponseException {
    public YnsRequestedRangeException(YnsHttpResponse response) {
        super(response, 416);
    }
}
