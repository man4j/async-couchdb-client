package com.equiron.yns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
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
