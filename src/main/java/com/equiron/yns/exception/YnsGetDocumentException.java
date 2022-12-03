package com.equiron.yns.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.yns.YnsDocIdAndRev;
import com.equiron.yns.json.YnsBulkGetErrorResult;

import lombok.Getter;

@Getter
public class YnsGetDocumentException extends RuntimeException {
    Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap = new HashMap<>();
    
    Map<String, YnsBulkGetErrorResult> errorDocumentsMap = new HashMap<>();
    
    public YnsGetDocumentException(Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap, 
                                   Map<String, YnsBulkGetErrorResult> errorDocumentsMap) {
        super("Conflicted documents: %s. Documents in error state: %".formatted(conflictingDocumentsMap.toString(), errorDocumentsMap.toString()));
        this.conflictingDocumentsMap = conflictingDocumentsMap;
        this.errorDocumentsMap = errorDocumentsMap;
    }
}
