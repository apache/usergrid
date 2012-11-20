package org.usergrid.persistence.cassandra.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as able to participate in a trace session
 *
 * @author zznate
 */
@Target(value = { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceParticipant {

    String name() default "";

}
