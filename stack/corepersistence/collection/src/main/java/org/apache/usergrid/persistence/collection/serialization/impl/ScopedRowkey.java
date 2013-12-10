package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.Scope;

import com.google.common.base.Preconditions;


/**
 * A row key that is within a scope.  Every I/O operation should be using this class.  No keys should be allowed that
 * aren't within a ScopedRowKey
 * @author tnine */
public class ScopedRowKey<K> {

    private final Scope scope;

    private final K key;


    public ScopedRowKey( final Scope scope, final K key ) {
        Preconditions.checkNotNull( scope, "Scope is required" );
        Preconditions.checkNotNull( key, "Key is required" );

        this.scope = scope;
        this.key = key;
    }


    /**
     * Get the stored scope
     * @return
     */
    public Scope getScope() {
        return scope;
    }


    /**
     * Get the suffix key
     * @return
     */
    public K getKey() {
        return key;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof ScopedRowKey ) ) {
            return false;
        }

        final ScopedRowKey that = ( ScopedRowKey ) o;

        if ( !key.equals( that.key ) ) {
            return false;
        }
        if ( !scope.equals( that.scope ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "ScopedRowKey{" +
                "scope=" + scope +
                ", key=" + key +
                '}';
    }


    /**
     * Utility function to generate a new key from the scope
     * @param scope
     * @param key
     * @param <K>
     * @return
     */
    public static <K> ScopedRowKey<K> fromKey(final Scope scope, K key){
        return new ScopedRowKey<K>( scope, key );
    }
}
