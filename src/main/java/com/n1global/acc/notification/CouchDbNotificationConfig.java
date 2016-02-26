package com.n1global.acc.notification;

import com.ning.http.client.AsyncHttpClient;

public class CouchDbNotificationConfig {
    private int heartbeatInMillis;

    private boolean includeDocs;

    private AsyncHttpClient httpClient;

    CouchDbNotificationConfig(int heartbeatInMillis, boolean includeDocs, AsyncHttpClient httpClient) {
        this.heartbeatInMillis = heartbeatInMillis;
        this.includeDocs = includeDocs;
        this.httpClient = httpClient;
    }

    public static class Builder {
        private int heartbeatInMillis = 30000;

        private boolean includeDocs;

        private AsyncHttpClient httpClient;

        public Builder setHeartbeatInMillis(int heartbeatInMillis) {
            this.heartbeatInMillis = heartbeatInMillis;

            return this;
        }

        public Builder setIncludeDocs(boolean includeDocs) {
            this.includeDocs = includeDocs;

            return this;
        }

        public Builder setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;

            return this;
        }

        public CouchDbNotificationConfig build() {
            return new CouchDbNotificationConfig(heartbeatInMillis, includeDocs, httpClient);
        }
    }

    public int getHeartbeatInMillis() {
        return heartbeatInMillis;
    }

    public boolean isIncludeDocs() {
        return includeDocs;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }
}
