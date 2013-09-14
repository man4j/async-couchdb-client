package com.n1global.acc.json.resultset;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.n1global.acc.json.CouchDbDocRev;

public abstract class CouchDbAbstractMapResultSet<K, V, ROW extends CouchDbMapRow<K, V>> extends CouchDbAbstractResultSet<K, V, ROW> {
    @JsonProperty("total_rows")
    private int totalRows;

    private int offset;

    public int getOffset() {
        return offset;
    }

    public int getTotalRows() {
        return totalRows;
    }

    @Override
    @JsonProperty("rows")
    public void setRows(List<ROW> rows) {
        for (ROW row : rows) {
            boolean deleted = false;

            /**
             * Extra rules for BuiltIn View for "keys" URL parameter:
             *
             * The row for a deleted document will have the revision ID of the deletion, and an extra key "deleted":true in the "value" property.
             * The row for a nonexistent document will just contain an "error" property with the value "not_found".
             *
             * http://wiki.apache.org/couchdb/HTTP_Bulk_Document_API#Fetch_Multiple_Documents_With_a_Single_Request
             */
            if (row.getValue() != null && row.getValue().getClass() == CouchDbDocRev.class) {//check for built-in views
                deleted = ((CouchDbDocRev) row.getValue()).isDeleted();
            }

            if (!row.isNotFound() && !deleted) {//check for built-in views
                this.getRows().add(row);
            }
        }
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>();

        for (ROW row : getRows()) {
            ids.add(row.getDocId());
        }

        return ids;
    }

    public String firstId() {
        return getRows().isEmpty() ? null : getRows().get(0).getDocId();
    }
}
