package com.equiron.acc.json;

import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility=Visibility.ANY,
                creatorVisibility=Visibility.NONE,
                getterVisibility=Visibility.NONE,
                isGetterVisibility=Visibility.NONE,
                setterVisibility=Visibility.NONE)
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

    public CouchDbDocumentAttachment(String contentType, String textData) {
        this.contentType = contentType;
        this.data = Base64.getEncoder().encodeToString(textData.getBytes());
    }
    
    public CouchDbDocumentAttachment(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = Base64.getEncoder().encodeToString(data);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTextData() {
        return new String(Base64.getDecoder().decode(data));
    }
    
    public byte[] getData() {
        return Base64.getDecoder().decode(data);
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
