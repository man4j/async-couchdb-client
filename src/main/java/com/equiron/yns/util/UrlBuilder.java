package com.equiron.yns.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.springframework.web.util.UriUtils;

public class UrlBuilder {
    private String prefix;

    private boolean pathEncodingFinished;

    public UrlBuilder(String prefix) {
        this.prefix = prefix;
    }

    public UrlBuilder addPathSegment(String pathSegment) {
        try {
            prefix += "/" + UriUtils.encodePathSegment(pathSegment, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public UrlBuilder addQueryParam(String name, String value) {
        if (!pathEncodingFinished) {
            prefix += "?";

            pathEncodingFinished = true;
        } else {
            prefix += "&";
        }

        try {
            prefix += URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public String toString() {
        return prefix;
    }

    public String build() {
        try {
            return new URL(prefix).toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
