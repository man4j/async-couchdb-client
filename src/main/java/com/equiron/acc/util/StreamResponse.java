package com.equiron.acc.util;

import java.io.InputStream;
import java.util.Map;

public class StreamResponse {
    private InputStream stream;
    
    private Map<String, String> headers;
    
    private int status;

    public StreamResponse(InputStream stream, Map<String, String> headers, int status) {
        this.stream = stream;
        this.headers = headers;
        this.status = status;
    }
    
    public InputStream getStream() {
        return stream;
    }

    public String getHeader(String header) {
        return headers.get(header.toLowerCase());
    }
    
    public int getStatus() {
        return status;
    }
}
