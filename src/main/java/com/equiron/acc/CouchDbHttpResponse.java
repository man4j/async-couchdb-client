package com.equiron.acc;

import io.netty.handler.codec.http.HttpHeaders;

public class CouchDbHttpResponse {
    private int statusCode;

    private String statusText;

    private String responseBody;

    private String requestUri;
    
    private HttpHeaders headers;

    public CouchDbHttpResponse(int statusCode, String statusText, String responseBody, String requestUri, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.responseBody = responseBody;
        this.requestUri = requestUri;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRequestUri() {
        return requestUri;
    }
    
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return statusCode + " / " + statusText + ". " + requestUri;
    }
}
