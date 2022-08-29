package com.equiron.yns.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.yns.YnsDocIdAndRev;
import com.equiron.yns.json.YnsBulkGetErrorResult;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsGetDocumentException extends RuntimeException {
    Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap = new HashMap<>();
    
    Map<String, YnsBulkGetErrorResult> errorDocumentsMap = new HashMap<>();
}
