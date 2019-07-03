package com.equiron.acc;

import org.asynchttpclient.AsyncHttpClient;

public class CouchDbConfig {
    private final String ip;
    
    private final int port;

    private final String user;

    private final String password;
    
    private final String dbName;

    private final AsyncHttpClient httpClient;
    
    private final boolean buildViewsOnStart;
    
    private final boolean selfDiscovering;
    
    CouchDbConfig(String ip, int port, String user, String password, String dbName, AsyncHttpClient httpClient, boolean buildViewsOnStart, boolean selfDiscovering) {
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
        this.httpClient = httpClient;
        this.buildViewsOnStart = buildViewsOnStart;
        this.selfDiscovering = selfDiscovering;
    }

    public static class Builder {
        String ip;
        
        int port = 5984;

        String user;

        String password;

        String dbName;

        AsyncHttpClient httpClient;
        
        boolean buildViewsOnStart = true;
        
        boolean selfDiscovering = true;
        
        public Builder setIp(String ip) {
            this.ip = ip;

            return this;
        }
        
        public Builder setPort(int port) {
            this.port = port;

            return this;
        }

        public Builder setUser(String user) {
            this.user = user;

            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;

            return this;
        }

        public Builder setDbName(String dbName) {
            this.dbName = dbName;
            
            return this;
        }

        public Builder setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;

            return this;
        }

        public Builder setBuildViewsOnStart(boolean buildViewsOnStart) {
            this.buildViewsOnStart = buildViewsOnStart;

            return this;
        }
        
        public Builder setSelfDiscovering(boolean selfDiscovering) {
            this.selfDiscovering = selfDiscovering;
            
            return this;
        }

        public CouchDbConfig build() {
            return new CouchDbConfig(ip, port, user, password, dbName, httpClient, buildViewsOnStart, selfDiscovering);
        }
    }
    
    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
    
    public String getDbName() {
        return dbName;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public boolean isBuildViewsOnStart() {
        return buildViewsOnStart;
    }
    
    public boolean isSelfDiscovering() {
        return selfDiscovering;
    }
}
