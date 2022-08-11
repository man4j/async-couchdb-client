package com.equiron.acc.exception;

import java.util.List;

import com.equiron.acc.json.YnsBulkResponse;

import lombok.Getter;

@Getter
public class YnsBulkRuntimeException extends RuntimeException {
    private List<YnsBulkResponse> responses;
    
    public YnsBulkRuntimeException(List<YnsBulkResponse> responses) {
        this.responses = responses;
    }
}
