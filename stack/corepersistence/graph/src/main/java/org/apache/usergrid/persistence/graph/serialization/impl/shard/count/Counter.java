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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;


/**
 * This class is synchronized for addition.  It is meant to be used across multiple threads
 */
public class Counter {
    /**
     * The counter to tell us how often it was invoked
     */
    private final AtomicLong invokeCounter;

    /**
     * Pointer to our "current" counter map.  We beginFlush this when time expires or we hit our count
     */
    private final ConcurrentHashMap<ShardKey, AtomicLong> counts;

    /**
     * The timestamp the concurrent map was created
     */
    private final long createTimestamp;


    /**
     * Implementation of the internal counters
     */
    public Counter() {
        this.createTimestamp = System.currentTimeMillis();
        this.invokeCounter = new AtomicLong();
        this.counts = new ConcurrentHashMap<>();
    }


    /**
     * Add the count to the key.
     */
    public void add( final ShardKey key, final long count ) {
        AtomicLong counter = counts.get( key );

        if ( counter == null ) {
            counter = new AtomicLong();
            AtomicLong existingCounter = counts.putIfAbsent( key, counter );

            if ( existingCounter != null ) {
                counter = existingCounter;
            }
        }

        counter.addAndGet( count );
        invokeCounter.incrementAndGet();
    }


    /**
     * Get the current valye from the cache
     */
    public long get( final ShardKey key ) {
        AtomicLong counter = counts.get( key );

        if ( counter == null ) {
            return 0;
        }

        return counter.get();
    }


    /**
     * Deep copy the counts from other into this counter
     * @param other
     */
    public void merge(final Counter other){

        Preconditions.checkNotNull(other, "other cannot be null");
        Preconditions.checkNotNull( other.counts, "other.counts cannot be null" );

        for(Map.Entry<ShardKey, AtomicLong> entry: other.counts.entrySet()){
            add(entry.getKey(), entry.getValue().get());
        }
    }


    /**
     * Get all entries
     * @return
     */
    public Set<Map.Entry<ShardKey, AtomicLong>> getEntries(){
        return counts.entrySet();
    }


    /**
     * Get the count of the number of times we've been incremented
     * @return
     */
    public long getInvokeCount() {
        return invokeCounter.get();
    }



    public long getCreateTimestamp() {
        return createTimestamp;
    }
}
