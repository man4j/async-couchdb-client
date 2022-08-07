package com.equiron.acc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public final class YnsMapReduceFunction {
    private String map;

    private String reduce;

    public YnsMapReduceFunction(String map, String reduce) {
        this.map = map;
        this.reduce = reduce;
    }

    public YnsMapReduceFunction(String map) {
        this.map = map;
    }
}
