package com.equiron.acc.util;

import java.util.Optional;
import java.util.stream.Stream;

public class EnvUtils {
    public static Optional<String> findEnvValue(String propName) {
        Optional<String> value = Stream.of(System.getProperty(propName),
                                           System.getProperty(propName.toLowerCase()), 
                                           System.getProperty(propName.toUpperCase()),
                                           System.getenv(propName),
                                           System.getenv(propName.toLowerCase()), 
                                           System.getenv(propName.toUpperCase())
                                           ).filter(v -> v != null).findFirst();
        return value;
    }
}
