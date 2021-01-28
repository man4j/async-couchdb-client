package com.equiron.acc.json;

import com.equiron.acc.CouchDb;

public class CouchDbDocumentAccessor {
    private CouchDbDocument document;

    public CouchDbDocumentAccessor(CouchDbDocument document) {
        this.document = document;
    }

    public CouchDbDocumentAccessor setInConflict(boolean inConflict) {
        document.inConflict = inConflict;

        return this;
    }

    public CouchDbDocumentAccessor setForbidden(boolean forbidden) {
        document.forbidden = forbidden;

        return this;
    }

    public CouchDbDocumentAccessor setConflictReason(String conflictReason) {
        document.conflictReason = conflictReason;

        return this;
    }
    
    public CouchDbDocumentAccessor setBulkError(String bulkError) {
        document.bulkError = bulkError;

        return this;
    }

    public CouchDbDocumentAccessor setCurrentDb(CouchDb currentDb) {
        document.currentDb = currentDb;

        return this;
    }
}
