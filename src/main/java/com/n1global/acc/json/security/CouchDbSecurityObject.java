package com.n1global.acc.json.security;

import java.util.Collections;
import java.util.Objects;

public class CouchDbSecurityObject {
    private CouchDbSecurityPattern admins = new CouchDbSecurityPattern(Collections.emptySet(), Collections.emptySet());
    
    private CouchDbSecurityPattern members = new CouchDbSecurityPattern(Collections.emptySet(), Collections.emptySet());

    public CouchDbSecurityPattern getAdmins() {
        return admins;
    }

    public void setAdmins(CouchDbSecurityPattern admins) {
        this.admins = admins;
    }

    public CouchDbSecurityPattern getMembers() {
        return members;
    }

    public void setMembers(CouchDbSecurityPattern members) {
        this.members = members;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof CouchDbSecurityObject) {
            CouchDbSecurityObject other = (CouchDbSecurityObject) obj;
            
            return admins.equals(other.admins) && members.equals(other.members);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(admins, members);
    }
}
