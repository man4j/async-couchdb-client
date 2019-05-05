package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchDbDocumentAttachment {
    @JsonProperty("content_type")
    private String contentType;

    private String data;

    private long length;

    private boolean stub;

    private String digest;

    public CouchDbDocumentAttachment() {
        /* empty */
    }

    public CouchDbDocumentAttachment(String contentType, String data) {
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getLength() {
        return length;
    }

    public boolean isStub() {
        return stub;
    }

    public String getDigest() {
        return digest;
    }
}
