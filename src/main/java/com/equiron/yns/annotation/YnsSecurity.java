package com.equiron.yns.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface YnsSecurity {
    /**
     * Admins have all the privileges of members plus the privileges: write (and edit) design documents, add/remove database admins and members, set the database revisions limit and execute temporary views against the database. They can not create a database nor delete a database. 
     */
    YnsSecurityPattern admins() default @YnsSecurityPattern(names = {}, roles = {"_admin"});
    
    /**
     * Members can read all types of documents from the DB, and they can write (and edit) documents to the DB except for design documents
     */
    YnsSecurityPattern members() default @YnsSecurityPattern(names = {}, roles = {"_admin"});
}
