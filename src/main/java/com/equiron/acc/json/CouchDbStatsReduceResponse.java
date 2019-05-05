package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbStatsReduceResponse {
    private double sum;
    
    private double count;
    
    private double min;
    
    private double max;

    public double getSum() {
        return sum;
    }

    public double getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
