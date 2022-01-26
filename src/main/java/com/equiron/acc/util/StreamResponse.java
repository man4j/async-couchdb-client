package com.equiron.acc.util;

import java.io.InputStream;
import java.util.Map;

public class StreamResponse {
    private InputStream stream;
    
    private Map<String, String> headers;

    public StreamResponse(InputStream stream, Map<String, String> headers) {
        this.stream = stream;
        this.headers = headers;
    }
    
    public InputStream getStream() {
        return stream;
    }

    public String getHeader(String header) {
        return headers.get(header.toLowerCase());
    }
}
