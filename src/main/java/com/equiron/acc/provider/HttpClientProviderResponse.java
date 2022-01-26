package com.equiron.acc.provider;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpClientProviderResponse {
    private final int status;

    private volatile String body;
    
    private volatile byte[] bodyAsBytes;
    
    private final String uri;
    
    private volatile InputStream in;
    
    private Map<String, String> headers;

    public HttpClientProviderResponse(int status, String body, String uri) {
        this.status = status;
        this.body = body;
        this.uri = uri;
    }
    
    public HttpClientProviderResponse(int status, byte[] body, String uri) {
        this.status = status;
        this.bodyAsBytes = body;
        this.uri = uri;
    }
    
    public HttpClientProviderResponse(int status, InputStream in, String uri, Map<String, String> headers) {
        this.status = status;
        this.in = in;
        this.uri = uri;
        
        Map<String, String> normalized = new HashMap<>();
        
        for (String key : headers.keySet()) {
            normalized.put(key.toLowerCase(), headers.get(key).toLowerCase());
        }
        
        this.headers = normalized;
    }
    
    public int getStatus() {
        return status;
    }
    
    public String getBody() {
        return body;
    }
    
    public byte[] getBodyAsBytes() {
        return bodyAsBytes;
    }
    
    public String getUri() {
        return uri;
    }
    
    public InputStream getIn() {
        return in;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
}
