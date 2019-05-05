package com.equiron.acc.json.resultset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CouchDbAbstractResultSet<K, V, ROW extends CouchDbAbstractRow<K, V>> {
    private List<ROW> rows = new ArrayList<>();

    @JsonProperty("rows")
    public void setRows(List<ROW> rows) {
        this.rows = rows;
    }

    public List<ROW> getRows() {
        return rows;
    }

    public List<K> keys() {
        return rows.stream().map(ROW::getKey).collect(Collectors.toList());
    }

    public List<V> values() {
        return rows.stream().map(ROW::getValue).collect(Collectors.toList());
    }

    public ROW firstRow() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    public K firstKey() {
        return rows.isEmpty() ? null : rows.get(0).getKey();
    }

    public V firstValue() {
        return rows.isEmpty() ? null : rows.get(0).getValue();
    }

    public LinkedHashMap<K, ROW> map() {
        return rows.stream().collect(Collectors.toMap(ROW::getKey, Function.identity(), (r1, r2) -> r1, LinkedHashMap::new));
    }

    public LinkedHashMap<K, List<ROW>> multiMap() {
        return rows.stream().collect(Collectors.groupingBy(ROW::getKey, LinkedHashMap::new, Collectors.toList()));
    }
}
