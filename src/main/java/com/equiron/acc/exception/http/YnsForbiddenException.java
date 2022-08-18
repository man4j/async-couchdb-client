package com.equiron.acc.exception.http;

import com.equiron.acc.YnsHttpResponse;
import com.equiron.acc.exception.YnsResponseException;

public class YnsForbiddenException extends YnsResponseException {
    public YnsForbiddenException(String message) {
        super(message, 403);
    }
    
    public YnsForbiddenException(YnsHttpResponse response) {
        super(response, 403);
    }
}