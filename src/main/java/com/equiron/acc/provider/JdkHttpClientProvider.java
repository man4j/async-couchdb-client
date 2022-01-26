package com.equiron.acc.provider;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.rainerhahnekamp.sneakythrow.Sneaky;

public class JdkHttpClientProvider implements HttpClientProvider {
    private HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(30)).build();
    
    private volatile HttpRequest.Builder prototype = HttpRequest.newBuilder().timeout(Duration.ofSeconds(30))
                                                                             .setHeader("Content-Type", "application/json; charset=utf-8");
    
    @Override
    public void setCredentials(String username, String password) {
        prototype.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
    }
    
    @Override
    public HttpClientProviderResponse post(String url, String body) {
        return post(url, body, null);
    }
    
    @Override
    public HttpClientProviderResponse put(String url, String body) {
        Builder builder = prototype.copy().PUT(BodyPublishers.ofString(body)).uri(URI.create(url));
        
        return exec(null, builder);
    }
    
    @Override
    public HttpClientProviderResponse put(String url, InputStream in, Map<String, String> headers) {
        Builder builder = prototype.copy().PUT(BodyPublishers.ofInputStream(() -> in)).uri(URI.create(url));
        
        return exec(headers, builder);
    }

    @Override
    public HttpClientProviderResponse post(String url, String body, Map<String, String> headers) {
        Builder builder = prototype.copy().POST(BodyPublishers.ofString(body)).uri(URI.create(url));
            
        return exec(headers, builder);
    }
    
    @Override
    public HttpClientProviderResponse get(String url) {
        return get(url, null);
    }

    @Override
    public HttpClientProviderResponse get(String url, Map<String, String> headers) {
        Builder builder = prototype.copy().GET().uri(URI.create(url));

        return exec(headers, builder);
    }
    
    private HttpClientProviderResponse exec(Map<String, String> headers, Builder builder) {
        return Sneaky.sneak(() -> {
            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> e : headers.entrySet()) {
                    builder.header(e.getKey(), e.getValue());
                }
            }
            
            HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString());
            
            return new HttpClientProviderResponse(response.statusCode(), response.body(), response.uri().toString());
        });
    }
    
    private HttpClientProviderResponse execBytes(Map<String, String> headers, Builder builder) {
        return Sneaky.sneak(() -> {
            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> e : headers.entrySet()) {
                    builder.header(e.getKey(), e.getValue());
                }
            }
            
            HttpResponse<byte[]> response = client.send(builder.build(), BodyHandlers.ofByteArray());
            
            return new HttpClientProviderResponse(response.statusCode(), response.body(), response.uri().toString());
        });
    }
    
    private HttpClientProviderResponse execStream(Map<String, String> headers, Builder builder) {
        return Sneaky.sneak(() -> {
            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> e : headers.entrySet()) {
                    builder.header(e.getKey(), e.getValue());
                }
            }
            
            HttpResponse<InputStream> response = client.send(builder.build(), BodyHandlers.ofInputStream());
            
            Map<String, String> responseHeaders = new HashMap<>();

            for (Entry<String, List<String>> e : response.headers().map().entrySet()) {
                String value = response.headers().firstValue(e.getKey()).orElse(null);
                
                if (value != null) {
                    responseHeaders.put(e.getKey(), value);
                }
            }
            
            return new HttpClientProviderResponse(response.statusCode(), response.body(), response.uri().toString(), responseHeaders);
        });
    }

    @Override
    public HttpClientProviderResponse delete(String url) {
        Builder builder = prototype.copy().DELETE().uri(URI.create(url));

        return exec(null, builder);
    }

    @Override
    public HttpClientProviderResponse stream(String url, String method, String body) {
        Builder builder;

        if (method.equals("GET")) {
            builder = prototype.copy().timeout(Duration.ofDays(9999)).GET().uri(URI.create(url));
        } else {
            builder = prototype.copy().timeout(Duration.ofDays(9999)).POST(BodyPublishers.ofString(body)).uri(URI.create(url));
        }
        
        return execStream(null, builder);
    }

    @Override
    public HttpClientProviderResponse getBytes(String url) {
        Builder builder = prototype.copy().GET().uri(URI.create(url));

        return execBytes(null, builder);
    }
}
