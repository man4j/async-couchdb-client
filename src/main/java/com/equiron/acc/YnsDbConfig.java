package com.equiron.acc;

import com.equiron.acc.provider.HttpClientProviderType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class YnsDbConfig {
    private final String host;
    
    private final int port;

    private final String user;

    private final String password;
    
    private final String dbName;

    private final boolean buildViewsOnStart;
    
    private final boolean selfDiscovering;
    
    private final boolean removeNotDeclaredReplications;
    
    private final int clientMaxParallelism;
    
    private final HttpClientProviderType httpClientProviderType;

    public static class Builder {
        String host;
        
        int port = 5984;

        String user;

        String password;

        String dbName;

        boolean buildViewsOnStart = true;
        
        boolean selfDiscovering = true;
        
        boolean removeNotDeclaredReplications = true;
        
        private int clientMaxParallelism = 128;
        
        HttpClientProviderType httpClientProviderType = HttpClientProviderType.JDK;
        
        public Builder setHost(String host) {
            this.host = host;

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

        public Builder setBuildViewsOnStart(boolean buildViewsOnStart) {
            this.buildViewsOnStart = buildViewsOnStart;

            return this;
        }
        
        public Builder setSelfDiscovering(boolean selfDiscovering) {
            this.selfDiscovering = selfDiscovering;
            
            return this;
        }
        
        public Builder setRemoveNotDeclaredReplications(boolean removeNotDeclaredReplications) {
            this.removeNotDeclaredReplications = removeNotDeclaredReplications;
            
            return this;
        }

        public Builder setClientMaxParallelism(int clientMaxParallelism) {
            this.clientMaxParallelism = clientMaxParallelism;
            
            return this;
        }
        
        public Builder setHttpClientProviderType(HttpClientProviderType httpClientProviderType) {
            this.httpClientProviderType = httpClientProviderType;
            
            return this;
        }

        public YnsDbConfig build() {
            return new YnsDbConfig(host, port, user, password, dbName, buildViewsOnStart, selfDiscovering, removeNotDeclaredReplications, clientMaxParallelism, httpClientProviderType);
        }
    }
}
