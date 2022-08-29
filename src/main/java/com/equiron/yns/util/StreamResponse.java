package com.equiron.yns.util;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class StreamResponse {
    private InputStream stream;
    
    private Map<String, String> headers;
    
    private int status;

    public StreamResponse(InputStream stream, Map<String, String> headers, int status) {
        this.stream = stream;
        this.headers = headers;
        this.status = status;
    }
    
    public String getHeader(String header) {
        return headers.get(header.toLowerCase());
    }
        
    @SneakyThrows
    public byte[] asBytes() {
        return IOUtils.toByteArray(stream);
    }
}
