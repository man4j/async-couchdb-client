package com.n1global.acc.json.resultset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
        List<K> keys = new ArrayList<>();

        for (ROW row : rows) {
            keys.add(row.getKey());
        }

        return keys;
    }

    public List<V> values() {
        List<V> values = new ArrayList<>();

        for (ROW row : rows) {
            values.add(row.getValue());
        }

        return values;
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
        LinkedHashMap<K, ROW> m = new LinkedHashMap<>();

        for (ROW row : rows) {
            m.put(row.getKey(), row);
        }

        return m;
    }

    public LinkedHashMap<K, List<ROW>> multiMap() {
        LinkedHashMap<K, List<ROW>> m = new LinkedHashMap<>();

        for (ROW row : rows) {
            if (!m.containsKey(row.getKey())) {
                m.put(row.getKey(), new ArrayList<ROW>());
            }

            m.get(row.getKey()).add(row);
        }

        return m;
    }
}
