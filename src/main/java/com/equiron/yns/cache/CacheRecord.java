package com.equiron.yns.cache;

import com.equiron.yns.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CacheRecord {
    private final String id;
    
    private YnsDocument doc;
}
