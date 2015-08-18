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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.util.GraphValidation;
import org.apache.usergrid.persistence.graph.serialization.util.IterableUtil;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/**
 * Simple implementation of the shard.  Uses a local Guava shard with a timeout.  If a value is not present in the
 * shard, it will need to be searched via cassandra.
 */
public class NodeShardCacheImpl implements NodeShardCache {

    private final NodeShardAllocation nodeShardAllocation;


    /**
     *  @param nodeShardAllocation
     */
    @Inject
    public NodeShardCacheImpl( final NodeShardAllocation nodeShardAllocation ) {
        Preconditions.checkNotNull( nodeShardAllocation, "nodeShardAllocation is required" );

        this.nodeShardAllocation = nodeShardAllocation;


    }


    @Override
    public ShardEntryGroup getWriteShardGroup( final ApplicationScope scope, final long timestamp,
                                               final DirectedEdgeMeta directedEdgeMeta ) {

        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final CacheKey key = new CacheKey( scope, directedEdgeMeta );
        CacheEntry entry;

        entry = getShards( key );


        final ShardEntryGroup shardId = entry.getShardId( timestamp );

        if ( shardId != null ) {
            return shardId;
        }

        //if we get here, something went wrong, our shard should always have a time UUID to return to us
        throw new GraphRuntimeException( "No time UUID shard was found and could not allocate one" );
    }


    @Override
    public Iterator<ShardEntryGroup> getReadShardGroup( final ApplicationScope scope, final long maxTimestamp,
                                                        final DirectedEdgeMeta directedEdgeMeta ) {
        ValidationUtils.validateApplicationScope( scope );
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final CacheKey key = new CacheKey( scope, directedEdgeMeta );
        CacheEntry entry;

        entry = getShards( key );

        Iterator<ShardEntryGroup> iterator = entry.getShards( maxTimestamp );

        if ( iterator == null ) {
            return Collections.<ShardEntryGroup>emptyList().iterator();
        }

        return iterator;
    }


    /**
     * Cache key for looking up items in the shard
     */
    private static class CacheKey {
        private final ApplicationScope scope;
        private final DirectedEdgeMeta directedEdgeMeta;


        private CacheKey( final ApplicationScope scope, final DirectedEdgeMeta directedEdgeMeta ) {
            this.scope = scope;
            this.directedEdgeMeta = directedEdgeMeta;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }

            final CacheKey cacheKey = ( CacheKey ) o;

            if ( !scope.equals( cacheKey.scope ) ) {
                return false;
            }

            if ( !directedEdgeMeta.equals( cacheKey.directedEdgeMeta ) ) {
                return false;
            }


            return true;
        }


        @Override
        public int hashCode() {
            int result = scope.hashCode();
            result = 31 * result + directedEdgeMeta.hashCode();
            return result;
        }
    }


    private CacheEntry getShards( final CacheKey key ) {
        final Iterator<ShardEntryGroup> edges =
            nodeShardAllocation.getShardsLocal( key.scope, Optional.<Shard>absent(), key.directedEdgeMeta );

        final CacheEntry cacheEntry = new CacheEntry( edges );

        return cacheEntry;
    }


    /**
     * An entry for the shard.
     */
    public static final class CacheEntry {
        /**
         * Get the list of all segments
         */
        private TreeMap<Long, ShardEntryGroup> shards;


        private CacheEntry( final Iterator<ShardEntryGroup> shards ) {
            Preconditions
                .checkArgument( shards.hasNext(), "More than 1 entry must be present in the shard to load into cache" );

            this.shards = new TreeMap<>();
            /**
             * TODO, we need to bound this.  While I don't envision more than a thousand groups max,
             * we don't want 1 entry to use all our ram
             */
            for ( ShardEntryGroup shard : IterableUtil.wrap( shards ) ) {
                this.shards.put( shard.getMinShard().getShardIndex(), shard );
            }
        }


        /**
         * Get all shards <= this one in descending order
         */
        public Iterator<ShardEntryGroup> getShards( final Long maxShard ) {

            final Long firstKey = shards.floorKey( maxShard );

            return Collections.unmodifiableCollection( shards.headMap( firstKey, true ).descendingMap().values() )
                              .iterator();
        }


        /**
         * Get the shard entry that should hold this value
         */
        public ShardEntryGroup getShardId( final Long seek ) {
            Map.Entry<Long, ShardEntryGroup> entry = shards.floorEntry( seek );

            if ( entry == null ) {
                throw new NullPointerException( "Entry should never be null, this is a bug" );
            }

            return entry.getValue();
        }
    }

}
