package org.apache.usergrid.persistence.collection.archaius;


import java.io.Serializable;
import java.lang.annotation.Annotation;

import com.google.inject.name.Named;


/**
 * A Named implementation which helps bind named Archaius dynamic properties to keys.
 */
@SuppressWarnings( "ClassExplicitlyAnnotation" )
public class NamedDynamicProperties implements Named, Serializable {
    private static final long serialVersionUID = 0;

    private final String value;


    public NamedDynamicProperties( String value ) {
        this.value = value;
    }


    public String value() {
        return this.value;
    }


    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return ( 127 * "value".hashCode() ) ^ value.hashCode();
    }


    public boolean equals( Object o ) {
        if ( !( o instanceof Named ) ) {
            return false;
        }

        Named other = ( Named ) o;
        return value.equals( other.value() );
    }


    public String toString() {
        return "@" + Named.class.getName() + "(value=" + value + ")";
    }


    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }
}
