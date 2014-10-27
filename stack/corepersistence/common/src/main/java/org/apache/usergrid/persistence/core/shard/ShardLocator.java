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
package org.apache.usergrid.persistence.core.shard;



import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;


/**
 * Simple utility to locate which bucket an element should be located based on it's funnel
 *
 */
public class ShardLocator<T> {

    /**
     * Use the murmur 3 hash
     */
    private static final HashFunction HASHER = Hashing.murmur3_128();

    private final int totalBuckets;
    private final Funnel<T> funnel;


    public ShardLocator( final Funnel<T> funnel, final int totalBuckets ) {
        this.funnel = funnel;
        this.totalBuckets = totalBuckets;
    }


    /**
     * Locate the bucket number given the value, the funnel and the total buckets.
     *
     * Assigns to {@code hashCode} a "bucket" in the range {@code [0, buckets)}, in a uniform manner that minimizes the
     * need for remapping as {@code buckets} grows. That is, {@code consistentHash(h, n)} equals:
     *
     * <ul> <li>{@code n - 1}, with approximate probability {@code 1/n} <li>{@code consistentHash(h, n - 1)}, otherwise
     * (probability {@code 1 - 1/n}) </ul>
     *
     * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">wikipedia article on consistent hashing</a>
     * for more information.
     *
     * <p>See <a href="http://arxiv.org/pdf/1406.2294v1.pdf">this paper</a> for more details on the algorithm</p>
     *
     *
     * Note that after testing, increasing buckets does NOT yield the expected results.  You will need an algorithm
     * that manually walks a tree.  See
     *
     */
    public int getBucket( T value ) {

        final HashCode hashCode = HASHER.hashObject( value, funnel );

        int owningIndex = Hashing.consistentHash( hashCode, totalBuckets );

        return owningIndex;
    }
}
