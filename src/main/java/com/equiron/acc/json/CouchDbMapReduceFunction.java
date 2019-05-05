package com.equiron.acc.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CouchDbMapReduceFunction {
    private String map;

    private String reduce;

    public CouchDbMapReduceFunction() {
        /* empty */
    }

    public CouchDbMapReduceFunction(String map, String reduce) {
        this.map = map;
        this.reduce = reduce;
    }

    public CouchDbMapReduceFunction(String map) {
        this.map = map;
    }

    public String getMap() {
        return map;
    }

    public String getReduce() {
        return reduce;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (this == obj) return true;

        CouchDbMapReduceFunction other = (CouchDbMapReduceFunction)obj;

        return Objects.equals(map, other.map) && Objects.equals(reduce, other.reduce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, reduce);
    }
}
