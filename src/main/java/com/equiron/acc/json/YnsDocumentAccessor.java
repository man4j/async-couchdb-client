package com.equiron.acc.json;

import com.equiron.acc.YnsDb;

public class YnsDocumentAccessor {
    private YnsDocument document;

    public YnsDocumentAccessor(YnsDocument document) {
        this.document = document;
    }

    public YnsDocumentAccessor setCurrentDb(YnsDb currentDb) {
        document.currentDb = currentDb;

        return this;
    }
}
