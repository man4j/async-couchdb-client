package com.n1global.acc.util;

public class NoopFunction<T> implements Function<T, T> {
    @Override
    public T apply(T input) {
        return input;
    }
}
