package com.n1global.acc.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.n1global.acc.exception.CouchDbResponseException;

public class ExceptionHandler {
    public static <T> T handleFutureResult(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Throwable originalException = e.getCause().getCause();

            if (originalException instanceof CouchDbResponseException) {
                originalException.fillInStackTrace();//for correct line number

                throw (CouchDbResponseException) originalException;
            }

            throw new RuntimeException(e);
        }
    }
}
