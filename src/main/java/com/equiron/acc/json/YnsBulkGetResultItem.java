package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = YnsBulkGetErrorResultItem.class)
public interface YnsBulkGetResultItem {
    //empty
}
