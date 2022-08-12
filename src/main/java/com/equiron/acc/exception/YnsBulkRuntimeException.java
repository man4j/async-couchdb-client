package com.equiron.acc.exception;

import java.util.List;

import com.equiron.acc.json.YnsBulkResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsBulkRuntimeException extends RuntimeException {
    private List<YnsBulkResponse> responses;
}
