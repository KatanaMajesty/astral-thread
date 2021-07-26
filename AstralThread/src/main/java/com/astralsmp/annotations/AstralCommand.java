package com.astralsmp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Target - куда накидывать аннотацию
 * Retention - время жизни
 * SOURCE - не даёт о себе знать IDE и компилятору
 * CLASS - во время работы компилятора
 * RUNTIME - во время runtime и компилятора
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AstralCommand {

    String cmdName();

    boolean lazyLoad() default false;

}
