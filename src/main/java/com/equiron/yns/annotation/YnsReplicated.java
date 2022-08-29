package com.equiron.yns.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(YnsReplications.class)
public @interface YnsReplicated {
    String targetHost();
    
    String targetPort() default "5984";
    
    String targetUser() default "";
    
    String targetPassword() default "";
    
    String targetDbName() default "";
    
    String targetProtocol() default "http";
    
    String selector() default "";
    
    String enabled() default "true";
    
    String createTarget() default "false";
    
    Direction direction() default Direction.BOTH; 
    
    public static enum Direction {
        BOTH, FROM, TO
    }
}
