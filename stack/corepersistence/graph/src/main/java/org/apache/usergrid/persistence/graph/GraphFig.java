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
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 *
 */
@FigSingleton
public interface GraphFig extends GuicyFig {


    String SCAN_PAGE_SIZE = "usergrid.graph.scan.page.size";

    String REPAIR_CONCURRENT_SIZE = "usergrid.graph.repair.concurrent.size";

    /**
     * The size of the shards.  This is approximate, and should be set lower than what you would like your max to be
     */
    String SHARD_SIZE = "usergrid.graph.shard.size";

    /**
     * The size of the worker count for shard auditing
     */
    String SHARD_AUDIT_QUEUE_SIZE = "usergrid.graph.shard.audit.worker.queue.size";


    /**
     * The size of the worker count for shard auditing
     */
    String SHARD_AUDIT_WORKERS = "usergrid.graph.shard.audit.worker.count";


    String SHARD_WRITE_CONSISTENCY = "usergrid.graph.shard.write.consistency";

    String SHARD_READ_CONSISTENCY = "usergrid.graph.shard.read.consistency";

    String LOCK_TTL = "usergrid.graph.shard.lock.ttl";

    String SHARD_REPAIR_CHANCE = "usergrid.graph.shard.repair.chance";


    String COUNTER_WRITE_FLUSH_COUNT = "usergrid.graph.shard.counter.beginFlush.count";

    String COUNTER_WRITE_FLUSH_INTERVAL = "usergrid.graph.shard.counter.beginFlush.interval";

    String COUNTER_WRITE_FLUSH_QUEUE_SIZE = "usergrid.graph.shard.counter.queue.size";


    @Default( "1000" )
    @Key( SCAN_PAGE_SIZE )
    int getScanPageSize();


    @Default( "5" )
    @Key( REPAIR_CONCURRENT_SIZE )
    int getRepairConcurrentSize();


    /**
     * A 1% repair chance.  On average we'll check to repair on 1 out of every 100 reads
     */
    @Default( ".01" )
    @Key( SHARD_REPAIR_CHANCE )
    double getShardRepairChance();

    @Default( "500000" )
    @Key( SHARD_SIZE )
    long getShardSize();

    @Default( "1" )
    //    @Default( "10" )
    @Key( SHARD_AUDIT_WORKERS )
    int getShardAuditWorkerCount();

    @Default( "1" )
    @Key( SHARD_AUDIT_QUEUE_SIZE )
    int getShardAuditWorkerQueueSize();


    @Default( "10000" )
    @Key( COUNTER_WRITE_FLUSH_COUNT )
    long getCounterFlushCount();


    @Default( "30000" )
    @Key( COUNTER_WRITE_FLUSH_INTERVAL )
    long getCounterFlushInterval();

    @Default( "1000" )
    @Key( COUNTER_WRITE_FLUSH_QUEUE_SIZE )
    int getCounterFlushQueueSize();

    @Default( "CL_EACH_QUORUM" )
    @Key( SHARD_WRITE_CONSISTENCY )
    String getShardWriteConsistency();

    /**
     * Get the consistency level for doing reads
     */
    @Default( "CL_LOCAL_QUORUM" )
    @Key( SHARD_READ_CONSISTENCY )
    String getShardReadConsistency();

    /**
     * Get the lock TTL in millis.  Our default is 30 seconds minute, much longer than we should need
     * @return
     */
    @Default( "30000" )
    @Key( LOCK_TTL )
    long getLockTTLMillis();
}

