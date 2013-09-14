package com.n1global.acc.transformer;

import com.n1global.acc.json.CouchDbBooleanResponse;
import com.n1global.acc.util.Function;

public class CouchDbBooleanResponseTransformer implements Function<CouchDbBooleanResponse, Boolean> {
    @Override
    public Boolean apply(CouchDbBooleanResponse input) {
        if (input == null) {
            return false;
        }

        return input.isOk();
    }
}
