package org.apache.usergrid.perftest.logging;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to use to inject a SLF4J Logger backed by Log4j.
 */
@Scope
@Documented
@Retention( RUNTIME )
@Target( FIELD )
public @interface Log {
}
