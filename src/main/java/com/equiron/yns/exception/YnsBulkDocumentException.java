package com.equiron.yns.exception;

import java.util.List;

import com.equiron.yns.json.YnsBulkResponse;

import lombok.Getter;

@Getter
public class YnsBulkDocumentException extends RuntimeException {
    private List<YnsBulkResponse> responses;
    
    public YnsBulkDocumentException(String message, List<YnsBulkResponse> responses) {
        super(message);
        
        this.responses = responses;
    }
}
