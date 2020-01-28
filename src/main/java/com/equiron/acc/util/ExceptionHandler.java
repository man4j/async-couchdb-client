package com.equiron.acc.util;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equiron.acc.exception.CouchDbTimeoutException;

public class ExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    public static <T> T handleFutureResult(Future<T> future) {
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            logger.error("CouchDb client catch exception: ", e);
            
            Throwable originalException = e;
            
            while (originalException.getCause() != null) {
                originalException = originalException.getCause();
            }
            
            originalException.fillInStackTrace();

            if (originalException instanceof RuntimeException) {
                throw (RuntimeException) originalException;
            }
            
            if (originalException instanceof HttpTimeoutException) {
                throw new CouchDbTimeoutException(originalException.getMessage());
            }

            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (@SuppressWarnings("unused") TimeoutException e) {
            throw new RuntimeException("CouchDb client unexpected future timeout");
        }
    }
}
