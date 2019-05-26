package com.equiron.acc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CouchDbConfig {
    String ip() default "";
    
    int port() default 0;
    
    String user() default "";
    
    String password() default "";
    
    String dbName() default "";
    
    boolean selfDiscovering() default true;
}
