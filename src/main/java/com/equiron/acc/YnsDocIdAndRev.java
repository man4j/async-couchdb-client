package com.equiron.acc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@JsonAutoDetect(fieldVisibility=Visibility.ANY,
                creatorVisibility=Visibility.NONE,
                getterVisibility=Visibility.NONE,
                isGetterVisibility=Visibility.NONE,
                setterVisibility=Visibility.NONE)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
public class YnsDocIdAndRev {
    @JsonProperty("_id")
    private String docId;

    @JsonProperty("_rev")
    private String rev;

    @JsonCreator
    public YnsDocIdAndRev(@JsonProperty("_id") String docId, 
                          @JsonProperty("_rev") String rev) {
        this.docId = docId;
        this.rev = rev;
    }
}