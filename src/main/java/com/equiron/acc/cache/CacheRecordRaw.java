package com.equiron.acc.cache;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CacheRecordRaw {
    private final String id;
    
    private Map<String, Object> doc;
}
