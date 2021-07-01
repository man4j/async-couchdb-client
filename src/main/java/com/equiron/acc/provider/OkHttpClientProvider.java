package com.equiron.acc.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.rainerhahnekamp.sneakythrow.Sneaky;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;

public class OkHttpClientProvider implements HttpClientProvider {
    private OkHttpClient client;
    
    private OkHttpClient listenerClient;
    
    public OkHttpClientProvider() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectTimeout(5, TimeUnit.SECONDS);
        okHttpClientBuilder.readTimeout(5, TimeUnit.SECONDS);
        okHttpClientBuilder.writeTimeout(5, TimeUnit.SECONDS);
        okHttpClientBuilder.callTimeout(30, TimeUnit.SECONDS);
        okHttpClientBuilder.authenticator(new MyAuthenticator());
        client = new OkHttpClient(okHttpClientBuilder);
        
        okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectTimeout(5000, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.readTimeout(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.writeTimeout(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.callTimeout(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.authenticator(new MyAuthenticator());
        listenerClient = new OkHttpClient(okHttpClientBuilder);
    }
    
    @Override
    public void setCredentials(String username, String password) {
        ((MyAuthenticator)client.authenticator()).setUsername(username);
        ((MyAuthenticator)client.authenticator()).setPassword(password);
        
        ((MyAuthenticator)listenerClient.authenticator()).setUsername(username);
        ((MyAuthenticator)listenerClient.authenticator()).setPassword(password);
    }
    
    @Override
    public HttpClientProviderResponse post(String url, String body) {
        return post(url, body, null);
    }
    
    @Override
    public HttpClientProviderResponse put(String url, String body) {
        Builder builder = new Request.Builder().put(RequestBody.create(body, MediaType.get("application/json; charset=utf-8"))).url(url);

        return exec(null, builder);
    }
    
    @Override
    public HttpClientProviderResponse put(String url, InputStream in, Map<String, String> headers) {
        Builder builder = Sneaky.sneak(() -> new Request.Builder().put(RequestBody.create(IOUtils.toByteArray(in), MediaType.get("application/json; charset=utf-8"))).url(url));

        return exec(null, builder);
    }

    @Override
    public HttpClientProviderResponse post(String url, String body, Map<String, String> headers) {
        Builder builder = new Request.Builder().post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8"))).url(url);

        return exec(headers, builder);
    }

    
    @Override
    public HttpClientProviderResponse get(String url) {
        return get(url, null);
    }
    
    @Override
    public HttpClientProviderResponse get(String url, Map<String, String> headers) {
        Builder builder = new Request.Builder().get().url(url);

        return exec(headers, builder);
    }
    
    @Override
    public HttpClientProviderResponse delete(String url) {
        Builder builder = new Request.Builder().delete().url(url);

        return exec(null, builder);
    }
    
    @Override
    public HttpClientProviderResponse stream(String url, String method, String body) {
        Builder builder;
        
        if (method.equals("GET")) {
            builder = new Request.Builder().get().url(url);
        } else {
            builder = new Request.Builder().post(RequestBody.create(body, MediaType.get("application/json; charset=utf-8"))).url(url);
        }

        return execStream(null, builder);
    }
    
    private HttpClientProviderResponse exec(Map<String, String> headers, Builder builder) {
        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
        }
        
        return Sneaky.sneak(() -> {
            try (Response response = client.newCall(builder.build()).execute(); ResponseBody body = response.body();) {          
                return new HttpClientProviderResponse(response.code(), body.string(), response.request().url().toString());
            }
        });
    }
    
    private HttpClientProviderResponse execBytes(Map<String, String> headers, Builder builder) {
        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
        }
        
        return Sneaky.sneak(() -> {
            try (Response response = client.newCall(builder.build()).execute(); ResponseBody body = response.body();) {          
                return new HttpClientProviderResponse(response.code(), body.bytes(), response.request().url().toString());
            }
        });
    }
    
    private HttpClientProviderResponse execStream(Map<String, String> headers, Builder builder) {
        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
        }
        
        return Sneaky.sneak(() -> {
            Response response = listenerClient.newCall(builder.build()).execute();
            return new HttpClientProviderResponse(response.code(), response.body().byteStream(), response.request().url().toString());
        });
    }

    class MyAuthenticator implements Authenticator {
        private volatile String username;
        
        private volatile String password;
        
        @Override
        public Request authenticate(Route arg0, Response response) throws IOException {
            if (username != null && password != null) {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder().header("Authorization", credential).build();
            }
            
            return response.request();
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @Override
    public HttpClientProviderResponse getBytes(String url) {
        Builder builder = new Request.Builder().get().url(url);

        return execBytes(null, builder);
    }
}
