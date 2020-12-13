package com.equiron.acc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Example 1: Name = proplists:get_value(<<"name">>, Doc, null), Id = proplists:get_value(<<"_id">>, Doc, null), Emit(Name, Id)
 * Example 2: Name = proplists:get_value(<<"name">>, Doc, null), Id = proplists:get_value(<<"_id">>, Doc, null), if Name == <<"Vova">> -> Emit(Name, Id); true -> ok end
 * Example 3: Name = proplists:get_value(<<\"name\">>, Doc, null), Id = proplists:get_value(<<\"_id\">>, Doc, null), if Name /= null -> Emit(Name, Id); true -> ok end
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ErlangView {
    String map();

    String reduce() default "";

    public final String COUNT = "_count";
    public final String SUM   = "_sum";
    public final String STATS = "_stats";
}
