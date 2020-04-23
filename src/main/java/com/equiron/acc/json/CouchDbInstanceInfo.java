package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbInstanceInfo {
    private String version;
    
    private String uuid;

    public String getVersion() {
        return version;
    }

    public String getUuid() {
        return uuid;
    }
}
