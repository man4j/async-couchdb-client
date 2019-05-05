package com.equiron.acc.json.security;

import java.util.HashSet;
import java.util.Set;

import com.equiron.acc.json.CouchDbDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CouchDbUser extends CouchDbDocument {
    private String name;
    
    private String password;
    
    private Set<String> roles = new HashSet<>();
    
    @SuppressWarnings("unused")
    private String type = "user";

    @JsonCreator
    public CouchDbUser(@JsonProperty("name") String name, @JsonProperty("password") String password, @JsonProperty("roles") Set<String> roles) {
        super("org.couchdb.user:" + name);
        
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getName() {
        return name;
    }
}
