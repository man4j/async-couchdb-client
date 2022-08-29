package com.equiron.yns.json;

import lombok.Getter;

@Getter
public class YnsBulkGetResultItem<T> {
    private T ok;
    
    private YnsBulkGetErrorResult error;
}
