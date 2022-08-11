package com.equiron.acc.cache;

import com.equiron.acc.json.YnsDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CacheRecord {
    private final String id;
    
    private volatile YnsDocument doc;
}
