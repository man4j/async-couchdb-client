package com.n1global.acc.notification.document;

import com.ning.http.client.AsyncHttpClient;

public class CouchDbDocumentListenerConfig {
    private int heartbeatInMillis;

    private int reConnectTimeout;

    private AsyncHttpClient httpClient;

    CouchDbDocumentListenerConfig(int heartbeatInMillis, int reConnectTimeout, AsyncHttpClient httpClient) {
        this.heartbeatInMillis = heartbeatInMillis;
        this.reConnectTimeout = reConnectTimeout;
        this.httpClient = httpClient;
    }

    public static class Builder {
        private int heartbeatInMillis = 30000;

        private int reConnectTimeout = 3000;

        private AsyncHttpClient httpClient;

        public Builder setHeartbeatInMillis(int heartbeatInMillis) {
            this.heartbeatInMillis = heartbeatInMillis;

            return this;
        }

        public Builder setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;

            return this;
        }

        public Builder setReconnectTimeout(int reConnectTimeout) {
            this.reConnectTimeout = reConnectTimeout;

            return this;
        }

        public CouchDbDocumentListenerConfig build() {
            return new CouchDbDocumentListenerConfig(heartbeatInMillis, reConnectTimeout, httpClient);
        }
    }

    public int getHeartbeatInMillis() {
        return heartbeatInMillis;
    }

    public int getReConnectTimeout() {
        return reConnectTimeout;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }
}
