package com.equiron.acc.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.equiron.acc.json.YnsBulkGetErrorResultItem;
import com.equiron.acc.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsBulkGetRuntimeException extends RuntimeException {
    Map<String, List<YnsDocument>> conflictingDocumentsMap = new HashMap<>();
    
    Map<String, YnsBulkGetErrorResultItem> errorDocumentsMap = new HashMap<>();
}
