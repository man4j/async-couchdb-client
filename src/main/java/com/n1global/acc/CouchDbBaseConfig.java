package com.n1global.acc;

import com.ning.http.client.AsyncHttpClient;

public class CouchDbBaseConfig {
    private String serverUrl;

    private String dbName = "";

    private String dbPrefix;

    private String user;

    private String password;

    private AsyncHttpClient httpClient;

    CouchDbBaseConfig(String serverUrl, String dbName, String dbPrefix, String user, String password, AsyncHttpClient httpClient) {
        this.serverUrl = serverUrl;
        this.dbName = dbName;
        this.dbPrefix = dbPrefix;
        this.user = user;
        this.password = password;
        this.httpClient = httpClient;
    }

    public static class Builder<T extends Builder<T>> {
        String serverUrl = "http://localhost:5984";

        String dbName;

        String dbPrefix = "";

        String user;

        String password;

        Class<T> derived;

        AsyncHttpClient httpClient;

        public Builder() {
            this.derived = (Class<T>) this.getClass();
        }

        public T setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;

            return derived.cast(this);
        }

        public T setDbName(String dbName) {
            this.dbName = dbName;

            return derived.cast(this);
        }

        public T setDbPrefix(String dbPrefix) {
            this.dbPrefix = dbPrefix;

            return derived.cast(this);
        }

        public T setUser(String user) {
            this.user = user;

            return derived.cast(this);
        }

        public T setPassword(String password) {
            this.password = password;

            return derived.cast(this);
        }

        public T setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;

            return derived.cast(this);
        }

        public CouchDbBaseConfig build() {
            return new CouchDbBaseConfig(serverUrl, dbName, dbPrefix, user, password, httpClient);
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
}
