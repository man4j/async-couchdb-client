package com.n1global.acc;

import com.ning.http.client.AsyncHttpClient;

public class CouchDbConfig {
    private final String serverUrl;

    private final String dbName;

    private final String dbPrefix;

    private final String user;

    private final String password;

    private final AsyncHttpClient httpClient;
    
    private final boolean buildViewsOnStart;

    private final boolean compactAllOnStart;
    
    private final boolean selfDiscovering;
    
    CouchDbConfig(String serverUrl, String dbName, String dbPrefix, String user, String password, AsyncHttpClient httpClient, boolean buildViewsOnStart, boolean compactAllOnStart, boolean selfDiscovering) {
        this.serverUrl = serverUrl;
        this.dbName = dbName;
        this.dbPrefix = dbPrefix;
        this.user = user;
        this.password = password;
        this.httpClient = httpClient;
        this.buildViewsOnStart = buildViewsOnStart;
        this.compactAllOnStart = compactAllOnStart;
        this.selfDiscovering = selfDiscovering;
    }

    public static class Builder {
        String serverUrl = "http://localhost:5984";

        String dbName;

        String dbPrefix = "";

        String user;

        String password;

        AsyncHttpClient httpClient;
        
        boolean selfDiscovering = true;

        boolean buildViewsOnStart = true;

        private boolean compactAllOnStart = false;//if true we have a CouchDb bug
        
        public Builder setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;

            return this;
        }

        public Builder setDbName(String dbName) {
            this.dbName = dbName;

            return this;
        }

        public Builder setDbPrefix(String dbPrefix) {
            this.dbPrefix = dbPrefix;

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

        public Builder setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;

            return this;
        }

        public Builder setSelfDiscovering(boolean selfDiscovering) {
            this.selfDiscovering = selfDiscovering;

            return this;
        }

        public Builder setBuildViewsOnStart(boolean buildViewsOnStart) {
            this.buildViewsOnStart = buildViewsOnStart;

            return this;
        }

        public Builder setCompactAllOnStart(boolean compactAllOnStart) {
            this.compactAllOnStart = compactAllOnStart;

            return this;
        }

        public CouchDbConfig build() {
            return new CouchDbConfig(serverUrl, dbName, dbPrefix, user, password, httpClient, buildViewsOnStart, compactAllOnStart, selfDiscovering);
        }
    }
    
    public String getServerUrl() {
        return serverUrl;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbPrefix() {
        return dbPrefix;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public boolean isBuildViewsOnStart() {
        return buildViewsOnStart;
    }

    public boolean isCompactAllOnStart() {
        return compactAllOnStart;
    }

    public boolean isSelfDiscovering() {
        return selfDiscovering;
    }
}
