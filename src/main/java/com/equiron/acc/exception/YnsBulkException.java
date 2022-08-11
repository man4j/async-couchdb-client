package com.equiron.acc.exception;

import java.util.List;

import com.equiron.acc.json.YnsBulkResponse;

import lombok.Getter;

@Getter
public class YnsBulkException extends Exception {
    private List<YnsBulkResponse> responses;
    
    public YnsBulkException(List<YnsBulkResponse> responses) {
        this.responses = responses;
    }
}
