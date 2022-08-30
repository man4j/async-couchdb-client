package com.equiron.yns.provider;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class HttpClientProviderResponse {
    private final int status;

    private volatile String body;
    
    private final String uri;
    
    private volatile InputStream in;
    
    private Map<String, String> headers;

    public HttpClientProviderResponse(String uri, int status, String body) {
        this.uri = uri;
        this.status = status;
        this.body = body;
    }
    
    public HttpClientProviderResponse(String uri, int status, InputStream in, Map<String, String> headers) {
        this.uri = uri;
        this.status = status;
        this.in = in;
        
        Map<String, String> normalized = new HashMap<>();
        
        for (String key : headers.keySet()) {
            normalized.put(key.toLowerCase(), headers.get(key));
        }
        
        this.headers = normalized;
    }
}