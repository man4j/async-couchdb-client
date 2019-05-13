package com.equiron.acc.tutorial.lesson6;

import java.util.Set;

import com.equiron.acc.json.security.CouchDbUser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomUser extends CouchDbUser {
    private String firstName;
    
    private String lastName;
    
    @JsonCreator
    public CustomUser(@JsonProperty("name") String name,
                      @JsonProperty("password") String password, 
                      @JsonProperty("roles") Set<String> roles) {
        super(name, password, roles);
    }

    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
