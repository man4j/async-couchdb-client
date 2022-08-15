package com.equiron.acc.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.acc.YnsDocIdAndRev;
import com.equiron.acc.json.YnsBulkGetErrorResult;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsBulkGetRuntimeException extends RuntimeException {
    Map<String, List<YnsDocIdAndRev>> conflictingDocumentsMap = new HashMap<>();
    
    Map<String, YnsBulkGetErrorResult> errorDocumentsMap = new HashMap<>();
}
