package com.equiron.yns.tutorial.lesson6;

import java.util.Set;

import com.equiron.yns.json.security.YnsUser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomUser extends YnsUser {
    private String firstName;
    
    private String lastName;
    
    @JsonCreator
    public CustomUser(@JsonProperty("name") String name,
                      @JsonProperty("password") String password, 
                      @JsonProperty("roles") Set<String> roles) {
        super(name, password, roles);
    }
}
