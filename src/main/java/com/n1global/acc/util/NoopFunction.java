package com.n1global.acc.util;

import java.util.function.Function;

public class NoopFunction<T> implements Function<T, T> {
    @Override
    public T apply(T input) {
        return input;
    }
}
