package com.equiron.acc.query;

import java.io.IOException;

import com.equiron.acc.util.UrlBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

public class YnsQueryObject<K> {
    private ObjectMapper mapper;

    @Getter
    @Setter
    private K[] keys;
    
    @Setter
    private K key;

    @Setter
    private boolean descending;

    @Setter
    private boolean isSetKey;

    @Setter
    private boolean isSetStartKey;

    @Setter
    private boolean isSetEndKey;

    @Setter
    private K endKey;

    @Setter
    private String endKeyDocId;

    @Setter
    private K startKey;

    @Setter
    private String startKeyDocId;

    @Setter
    private boolean group;

    @Setter
    private int groupLevel;

    @Setter
    private boolean inclusiveEnd = true;

    @Getter
    @Setter
    private int limit;

    @Setter
    private int skip;

    @Setter
    private boolean reduce;

    public YnsQueryObject(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String toQuery() throws IOException {
        UrlBuilder urlBuilder = new UrlBuilder("");

        if (isSetKey) urlBuilder.addQueryParam("key", mapper.writeValueAsString(key));

        if (isSetEndKey) urlBuilder.addQueryParam("endkey", mapper.writeValueAsString(endKey));

        if (endKeyDocId != null) urlBuilder.addQueryParam("endkey_docid", endKeyDocId);

        if (isSetStartKey) urlBuilder.addQueryParam("startkey", mapper.writeValueAsString(startKey));

        if (startKeyDocId != null) urlBuilder.addQueryParam("startkey_docid", startKeyDocId);

        if (descending) urlBuilder.addQueryParam("descending", "true");

        if (group) urlBuilder.addQueryParam("group", "true");

        if (groupLevel != 0) urlBuilder.addQueryParam("group_level", groupLevel + "");

        if (!inclusiveEnd) urlBuilder.addQueryParam("inclusive_end", "false");
        
        if (limit != 0) urlBuilder.addQueryParam("limit", limit + "");

        urlBuilder.addQueryParam("stable", "true");

        if (skip != 0) urlBuilder.addQueryParam("skip", skip + "");

        if (!reduce) urlBuilder.addQueryParam("reduce", "false");

        return urlBuilder.toString();
    }

    public String jsonKeys() throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writeValueAsString(new Object() {
            @SuppressWarnings({ "hiding" })
            @JsonProperty("keys")
            K[] keys = YnsQueryObject.this.keys;
        });
    }
}
