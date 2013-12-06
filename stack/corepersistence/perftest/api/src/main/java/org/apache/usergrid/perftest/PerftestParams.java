package org.apache.usergrid.perftest;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Method level annotation to enable performance testing.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface PerftestParams {
    long callCount();
    int threadCount();
    long delayBetweenCalls() default 0;
    Class[] modules();
}
