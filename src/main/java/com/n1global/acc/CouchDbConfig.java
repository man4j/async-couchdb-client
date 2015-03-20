package com.n1global.acc;

import com.ning.http.client.AsyncHttpClient;

public class CouchDbConfig extends CouchDbBaseConfig {
    private boolean buildViewsOnStart;

    private boolean compactAllOnStart;
    
    private boolean selfDiscovering = true;

    CouchDbConfig(String dbUrl, String dbName, String dbPrefix, String user, String password, AsyncHttpClient httpClient, boolean buildViewsOnStart, boolean compactAllOnStart, boolean selfDiscovering) {
        super (dbUrl, dbName, dbPrefix, user, password, httpClient);

        this.buildViewsOnStart = buildViewsOnStart;
        this.compactAllOnStart = compactAllOnStart;
        this.selfDiscovering = selfDiscovering;
    }

    public static class Builder extends CouchDbBaseConfig.Builder<Builder> {
        boolean selfDiscovering = true;

        boolean buildViewsOnStart = true;

        private boolean compactAllOnStart = false;//if true we have a CouchDb bug

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

        @Override
        public CouchDbConfig build() {
            return new CouchDbConfig(serverUrl, dbName, dbPrefix, user, password, httpClient, buildViewsOnStart, compactAllOnStart, selfDiscovering);
        }
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
