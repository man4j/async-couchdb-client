package com.n1global.acc.json.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CouchDbSecurityPattern {
    private Set<String> names = new HashSet<>();
    
    private Set<String> roles = new HashSet<>();
    
    public CouchDbSecurityPattern(String[] names, String[] roles) {
        this.names = new HashSet<>(Arrays.asList(names));
        this.roles = new HashSet<>(Arrays.asList(roles));
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
