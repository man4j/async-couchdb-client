package com.equiron.acc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Replicated {
    String targetIp();
    
    int targetPort() default 5984;
    
    String targetUser() default "";
    
    String targetPassword() default "";
    
    String targetDbName() default "";
    
    String selector() default "";
}
