package com.equiron.acc.json.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbSecurityPattern {
    private Set<String> names = new HashSet<>();
    
    private Set<String> roles = new HashSet<>();
    
    @JsonCreator
    public CouchDbSecurityPattern(@JsonProperty("names") Set<String> names, @JsonProperty("roles") Set<String> roles) {
        this.names = names;
        this.roles = roles;
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof CouchDbSecurityPattern) {
            CouchDbSecurityPattern other = (CouchDbSecurityPattern) obj;
            
            return names.equals(other.names) && roles.equals(other.roles);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(names, roles);
    }
}
