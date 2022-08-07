package com.equiron.acc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.equiron.acc.provider.HttpClientProviderType;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface YnsDbConfig {
    String host() default "";
    
    String port() default "";
    
    String user() default "";
    
    String password() default "";
    
    String dbName() default "";
    
    String clientMaxParallelism() default "";

    boolean selfDiscovering() default true;
    
    boolean buildViewOnStart() default true;
    
    boolean removeNotDeclaredReplications() default true;
    
    HttpClientProviderType httpClientProviderType() default HttpClientProviderType.JDK;
}
