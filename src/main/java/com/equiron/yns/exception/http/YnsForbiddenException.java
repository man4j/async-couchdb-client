package com.equiron.yns.exception.http;

import com.equiron.yns.YnsHttpResponse;

public class YnsForbiddenException extends YnsResponseException {
    public YnsForbiddenException(String message) {
        super(message, 403);
    }
    
    public YnsForbiddenException(YnsHttpResponse response) {
        super(response, 403);
    }
}
