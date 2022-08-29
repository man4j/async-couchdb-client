package com.equiron.yns.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class YnsStatsReduceResponse {
    private double sum;
    
    private double count;
    
    private double min;
    
    private double max;
}
