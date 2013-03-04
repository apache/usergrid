package org.usergrid.cassandra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zznate
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataControl {
    boolean skipTruncate() default false; // skips truncate on exit
    String schemaManager(); // returns the loader impl
}
