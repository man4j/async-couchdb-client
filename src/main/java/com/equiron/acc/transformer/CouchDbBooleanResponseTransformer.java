package com.equiron.acc.transformer;

import java.util.function.Function;

import com.equiron.acc.json.CouchDbBooleanResponse;

public class CouchDbBooleanResponseTransformer implements Function<CouchDbBooleanResponse, Boolean> {
    @Override
    public Boolean apply(CouchDbBooleanResponse input) {
        if (input == null) {
            return false;
        }

        return input.isOk();
    }
}
