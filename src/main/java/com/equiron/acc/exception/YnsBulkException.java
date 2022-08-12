package com.equiron.acc.exception;

import java.util.List;

import com.equiron.acc.json.YnsBulkResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YnsBulkException extends Exception {
    private List<YnsBulkResponse> responses;
}
