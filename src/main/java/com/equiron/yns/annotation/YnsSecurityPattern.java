package com.equiron.yns.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface YnsSecurityPattern {
    /**
     * List user names.
     */
    String[] names() default {};
    
    /**
     * List of users roles. 
     */
    String[] roles() default {};
}
