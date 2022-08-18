package com.equiron.acc.cache;

import com.equiron.acc.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CacheRecord {
    private final String id;
    
    private YnsDocument doc;
}