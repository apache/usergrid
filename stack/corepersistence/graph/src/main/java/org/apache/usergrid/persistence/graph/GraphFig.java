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
package org.apache.usergrid.persistence.graph;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 *
 */
public interface GraphFig extends GuicyFig {


    public static final String SCAN_PAGE_SIZE = "usergrid.graph.scan.page.size";

    public static final String REPAIR_CONCURRENT_SIZE = "usergrid.graph.repair.concurrent.size";

    public static final String SHARD_SIZE = "usergrid.graph.shard.size";

    public static final String SHARD_CACHE_SIZE = "usergrid.graph.shard.cache.size";


    /**
     * Get the cache timeout.  The local cache will exist for this amount of time max (in millis).
     */
    public static final String SHARD_CACHE_TIMEOUT = "usergrid.graph.shard.cache.timeout";

    /**
     * The minimum amount of time than can occur (in millis) between shard allocation.  Must be at least 2x the cache timeout.
     *
     * Note that you should also pad this for node clock drift.  A good value for this would be 2x the shard cache timeout + 30 seconds,
     * assuming you have NTP and allow a max drift of 30 seconds
     */
    public static final String SHARD_MIN_DELTA = "usergrid.graph.shard.min.delta";


    public static final String COUNTER_WRITE_FLUSH_COUNT = "usergrid.graph.shard.counter.beginFlush.count";

    public static final String COUNTER_WRITE_FLUSH_INTERVAL = "usergrid.graph.shard.counter.beginFlush.interval";

    public static final String COUNTER_WRITE_FLUSH_QUEUE_SIZE = "usergrid.graph.shard.counter.queue.size";




    @Default("1000")
    @Key(SCAN_PAGE_SIZE)
    int getScanPageSize();



    @Default("5")
    @Key(REPAIR_CONCURRENT_SIZE)
    int getRepairConcurrentSize();



    @Default("500000")
    @Key(SHARD_SIZE)
    long getShardSize();


    @Default("30000")
    @Key(SHARD_CACHE_TIMEOUT)
    long getShardCacheTimeout();

    @Default("60000")
    @Key( SHARD_MIN_DELTA )
    long getShardMinDelta();


    @Default( "250000" )
    @Key( SHARD_CACHE_SIZE )
    long getShardCacheSize();




    @Default( "10000" )
    @Key( COUNTER_WRITE_FLUSH_COUNT )
    long getCounterFlushCount();


    @Default( "30000" )
    @Key( COUNTER_WRITE_FLUSH_INTERVAL )
    long getCounterFlushInterval();

    @Default( "1000" )
    @Key(COUNTER_WRITE_FLUSH_QUEUE_SIZE  )
    int getCounterFlushQueueSize();
}

