package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsRequestedRangeException extends YnsResponseException {
    public YnsRequestedRangeException(YnsHttpResponse response) {
        super(response, 416);
    }
}
