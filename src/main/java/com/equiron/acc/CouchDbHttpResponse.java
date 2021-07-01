package com.equiron.acc;

public class CouchDbHttpResponse {
    private int statusCode;

    private String statusText;

    private String responseBody;
    
    private String requestUri;
    
    public CouchDbHttpResponse(int statusCode, String statusText, String responseBody, String requestUri) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.responseBody = responseBody;
        this.requestUri = requestUri;
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
    
    @Override
    public String toString() {
        return statusCode + " / " + statusText + ". " + requestUri;
    }
}
