package com.equiron.yns.exception;

import java.util.List;

import com.equiron.yns.json.YnsBulkResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsBulkDocumentException extends RuntimeException {
    private List<YnsBulkResponse> responses;
}
