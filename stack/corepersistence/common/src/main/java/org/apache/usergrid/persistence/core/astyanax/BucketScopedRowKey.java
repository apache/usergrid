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
import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A scoped row key that also includes an index, which can be used for consistent hashing.
 */
public class BucketScopedRowKey<K> extends ScopedRowKey<K> {

    private final int bucketNumber;


    /**
     * Create a scoped row key, with the funnel for determining the bucket
     *
     * @param bucketNumber The bucket number for this row key
     */
    public BucketScopedRowKey( final Id scope, final K key, int bucketNumber ) {
        super( scope, key );
        this.bucketNumber = bucketNumber;
    }


    /**
     * Get the bucket number
     */
    public int getBucketNumber() {
        return bucketNumber;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof BucketScopedRowKey ) ) {
            return false;
        }
        if ( !super.equals( o ) ) {
            return false;
        }

        final BucketScopedRowKey that = ( BucketScopedRowKey ) o;

        if ( bucketNumber != that.bucketNumber ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + bucketNumber;
        return result;
    }


    @Override
    public String toString() {
        return "BucketScopedRowKey{" +
                "bucketNumber=" + bucketNumber +
                "} " + super.toString();
    }


    /**
     * Utility function to generate a new key from the scope
     */
    public static <K> BucketScopedRowKey<K> fromKey( final Id scope, final K key, final int bucketNumber ) {
        return new BucketScopedRowKey<>( scope, key, bucketNumber );
    }


    /**
     * Create a list of all buckets from [0,  totalBuckets}.  Note that this is an n-1 0 based system
     */
    public static <K> List<BucketScopedRowKey<K>> fromRange( final Id scope, final K key, final int... buckets ) {

        final List<BucketScopedRowKey<K>> results = new ArrayList<>( buckets.length  );


        for ( int i = 0; i < buckets.length; i++ ) {
            results.add( new BucketScopedRowKey<>( scope, key, buckets[i] ) );
        }

        return results;
    }
}
