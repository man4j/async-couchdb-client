package com.equiron.yns.json.resultset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.equiron.yns.json.YnsDocRev;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class YnsMapResultSet<K, V> extends YnsAbstractResultSet<K, V, YnsMapRow<K, V>> {
    @JsonProperty("total_rows")
    @Getter
    private int totalRows;

    @Getter
    private int offset;

    @Override
    @JsonProperty("rows")
    public void setRows(List<YnsMapRow<K, V>> rows) {
        for (var row : rows) {
            boolean deleted = false;

            /**
             * Extra rules for BuiltIn View for "keys" URL parameter:
             *
             * The row for a deleted document will have the revision ID of the deletion, and an extra key "deleted":true in the "value" property.
             * The row for a nonexistent document will just contain an "error" property with the value "not_found".
             *
             * http://wiki.apache.org/couchdb/HTTP_Bulk_Document_API#Fetch_Multiple_Documents_With_a_Single_Request
             */
            if (row.getValue() != null && row.getValue().getClass() == YnsDocRev.class) {//check for built-in views
                deleted = ((YnsDocRev) row.getValue()).isDeleted();
            }

            if (!row.isNotFound() && !deleted) {//check for built-in views
                this.getRows().add(row);
            }
        }
    }

    public List<String> ids() {
        return getRows().stream().map(YnsMapRow::getDocId).collect(Collectors.toCollection(ArrayList::new));
    }

    public String firstId() {
        return getRows().isEmpty() ? null : getRows().get(0).getDocId();
    }
}