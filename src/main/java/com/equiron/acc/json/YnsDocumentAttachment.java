package com.equiron.acc.json;

import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility=Visibility.ANY,
                creatorVisibility=Visibility.NONE,
                getterVisibility=Visibility.NONE,
                isGetterVisibility=Visibility.NONE,
                setterVisibility=Visibility.NONE)
@JsonInclude(Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class YnsDocumentAttachment {
    @JsonProperty("content_type")
    @Getter
    @Setter
    private String contentType;

    @Setter
    private String data;

    @Getter
    private long length;

    @Getter
    private boolean stub;

    @Getter
    private String digest;

    public YnsDocumentAttachment(String contentType, String textData) {
        this.contentType = contentType;
        this.data = Base64.getEncoder().encodeToString(textData.getBytes());
    }
    
    public YnsDocumentAttachment(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = Base64.getEncoder().encodeToString(data);
    }

    public String getTextData() {
        return new String(Base64.getDecoder().decode(data));
    }
    
    public byte[] getData() {
        return Base64.getDecoder().decode(data);
    }
}
