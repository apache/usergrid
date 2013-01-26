package org.usergrid.cassandra;

/**
 * @author zznate
 */
public @interface DataControl {
    boolean skipTruncate() default false; // skips truncate on exit
    String loader(); // returns the loader impl
}
