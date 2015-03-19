package com.n1global.acc.json.security;

public class CouchDbSecurityObject {
    private CouchDbSecurityPattern admins;
    
    private CouchDbSecurityPattern members;

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
}
