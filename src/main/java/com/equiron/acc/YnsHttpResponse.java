package com.equiron.acc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class YnsHttpResponse {
    private int statusCode;

    private String statusText;

    private String responseBody;
    
    private String requestUri;
}
