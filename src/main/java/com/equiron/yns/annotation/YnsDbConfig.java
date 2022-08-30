package com.equiron.yns.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.equiron.yns.annotation.model.AnnotationConfigOption;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface YnsDbConfig {
    String host() default "";
    
    String port() default "";
    
    String user() default "";
    
    String password() default "";
    
    String dbName() default "";
    
    String clientMaxParallelism() default "";
    
    String cacheMaxDocsCount() default "";

    String cacheMaxTimeoutSec() default "";

    AnnotationConfigOption selfDiscovering() default AnnotationConfigOption.BY_CONFIG;
    
    AnnotationConfigOption buildViewOnStart() default AnnotationConfigOption.BY_CONFIG;
    
    AnnotationConfigOption removeNotDeclaredReplications() default AnnotationConfigOption.BY_CONFIG;
    
    AnnotationConfigOption enableDocumentCache() default AnnotationConfigOption.BY_CONFIG;
}