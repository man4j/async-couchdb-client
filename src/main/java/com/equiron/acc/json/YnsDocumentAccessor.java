package com.equiron.acc.json;

import com.equiron.acc.YnsDb;

public class YnsDocumentAccessor {
    private YnsDocument document;

    public YnsDocumentAccessor(YnsDocument document) {
        this.document = document;
    }

    public YnsDocumentAccessor setInConflict(boolean inConflict) {
        document.inConflict = inConflict;

        return this;
    }

    public YnsDocumentAccessor setForbidden(boolean forbidden) {
        document.forbidden = forbidden;

        return this;
    }

    public YnsDocumentAccessor setConflictReason(String conflictReason) {
        document.conflictReason = conflictReason;

        return this;
    }
    
    public YnsDocumentAccessor setBulkError(String bulkError) {
        document.bulkError = bulkError;

        return this;
    }

    public YnsDocumentAccessor setCurrentDb(YnsDb currentDb) {
        document.currentDb = currentDb;

        return this;
    }
}
