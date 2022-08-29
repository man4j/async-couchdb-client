package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(Include.NON_NULL)
@AllArgsConstructor
public class YnsDocIdRevRequestItem {
    private String id;

    private String rev;
}
