package com.equiron.acc.provider;

import java.io.InputStream;
import java.util.Map;

public interface HttpClientProvider {
    void setCredentials(String username, String password);
    
    HttpClientProviderResponse get(String url, Map<String, String> headers);
    
    HttpClientProviderResponse post(String url, String body);
    
    HttpClientProviderResponse put(String url, String body);
    
    HttpClientProviderResponse put(String url, InputStream in, Map<String, String> headers);
    
    HttpClientProviderResponse get(String url);
    
    HttpClientProviderResponse delete(String url);
    
    HttpClientProviderResponse getStream(String url, String method, String body, Map<String, String> headers);
}
