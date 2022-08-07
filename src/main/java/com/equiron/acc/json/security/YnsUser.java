package com.equiron.acc.json.security;

import java.util.HashSet;
import java.util.Set;

import com.equiron.acc.json.YnsDocument;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

public class YnsUser extends YnsDocument {
    public static final String COUCHDB_USER_PREFIX = "org.couchdb.user:";

    @Getter
    private String name;
    
    @Getter
    @Setter
    private String password;
    
    @Getter
    @Setter
    private Set<String> roles = new HashSet<>();
    
    @SuppressWarnings("unused")
    private String type = "user";

    @JsonCreator
    public YnsUser(@JsonProperty("name") String name, @JsonProperty("password") String password, @JsonProperty("roles") Set<String> roles) {
        super(COUCHDB_USER_PREFIX + name);
        
        this.name = name;
        this.password = password;
        this.roles = roles;
    }
}
