package com.equiron.acc.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.equiron.acc.exception.CouchDbResponseException;

public class ExceptionHandler {
    public static <T> T handleFutureResult(Future<T> future) {
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause().getCause() != null) {
                Throwable originalException = e.getCause().getCause();
    
                if (originalException instanceof CouchDbResponseException) {
                    originalException.fillInStackTrace();//for correct line number
    
                    throw (CouchDbResponseException) originalException;
                } 
            }

            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("CouchDB async future timeout!", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
