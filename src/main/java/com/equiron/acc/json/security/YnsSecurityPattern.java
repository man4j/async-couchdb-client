package com.equiron.acc.json.security;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YnsSecurityPattern {
    private Set<String> names = new HashSet<>();
    
    private Set<String> roles = new HashSet<>();
}
