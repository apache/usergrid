/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.astyanax;


import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * A row key that is within a Scope.  Every I/O operation should be using this class.  
 * No keys should be allowed that aren't within a Scope
 *
 * @author tnine
 */
public class ScopedRowKey< K> {

    private final Id scope;

    private final K key;


    public ScopedRowKey( final Id scope, final K key ) {
        Preconditions.checkNotNull( scope, "CollectionScope is required" );
        Preconditions.checkNotNull( key, "Key is required" );

        this.scope = scope;
        this.key = key;
    }


    /**
     * Get the stored scope
     */
    public Id getScope() {
        return scope;
    }


    /**
     * Get the suffix key
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
     */
    public static <K> ScopedRowKey< K> fromKey( final Id scope, K key ) {
        return new ScopedRowKey<>( scope, key );
    }
}
