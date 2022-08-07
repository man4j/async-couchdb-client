package com.equiron.acc.transformer;

import java.util.function.Function;

import com.equiron.acc.json.YnsBooleanResponse;

public class YnsBooleanResponseTransformer implements Function<YnsBooleanResponse, Boolean> {
    @Override
    public Boolean apply(YnsBooleanResponse input) {
        if (input == null) {
            return false;
        }

        return input.isOk();
    }
}
