package com.equiron.acc.json.security;

import java.util.Set;

import lombok.Data;

@Data
public class YnsSecurityObject {
    private YnsSecurityPattern admins = new YnsSecurityPattern(Set.of(), Set.of("_admin"));
    
    private YnsSecurityPattern members = new YnsSecurityPattern(Set.of(), Set.of("_admin"));
}
