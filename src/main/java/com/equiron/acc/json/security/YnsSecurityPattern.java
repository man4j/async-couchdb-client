package com.equiron.acc.json.security;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class YnsSecurityPattern {
    private Set<String> names = new HashSet<>();
    
    private Set<String> roles = new HashSet<>();
    
    @JsonCreator
    public YnsSecurityPattern(@JsonProperty("names") Set<String> names, @JsonProperty("roles") Set<String> roles) {
        this.names = names;
        this.roles = roles;
    }
}
