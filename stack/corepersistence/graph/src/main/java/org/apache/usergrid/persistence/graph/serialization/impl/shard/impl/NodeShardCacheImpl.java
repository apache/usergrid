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


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.graph.GraphFig;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.google.inject.Inject;


/**
 * Simple implementation of the shard.  Uses a local Guava shard with a timeout.  If a value is not present in the
 * shard, it will need to be searched via cassandra.
 */
public class NodeShardCacheImpl implements NodeShardCache {

    private static final Logger LOG = LoggerFactory.getLogger( NodeShardCacheImpl.class );

    /**
     * Only cache shards that have < 10k groups.  This is an arbitrary amount, and may change with profiling and
     * testing
     */
    private static final int MAX_WEIGHT_PER_ELEMENT = 10000;

    private final NodeShardAllocation nodeShardAllocation;
    private final GraphFig graphFig;
    private final TimeService timeservice;

    private LoadingCache<CacheKey, CacheEntry> graphs;


    /**
     *  @param nodeShardAllocation
     * @param graphFig
     * @param timeservice
     */
    @Inject
    public NodeShardCacheImpl( final NodeShardAllocation nodeShardAllocation, final GraphFig graphFig,
                               final TimeService timeservice ) {

        Preconditions.checkNotNull( nodeShardAllocation, "nodeShardAllocation is required" );
        Preconditions.checkNotNull( graphFig, "consistencyFig is required" );
        Preconditions.checkNotNull( timeservice, "timeservice is required" );

        this.nodeShardAllocation = nodeShardAllocation;
        this.graphFig = graphFig;
        this.timeservice = timeservice;

        /**
         * Add our listener to reconstruct the shard
         */
        this.graphFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propertyName = evt.getPropertyName();

                if ( propertyName.equals( GraphFig.SHARD_CACHE_SIZE ) || propertyName
                        .equals( GraphFig.SHARD_CACHE_TIMEOUT ) ) {

                    updateCache();
                }
            }
        } );

        /**
         * Initialize the shard cache
         */
        updateCache();
    }


    @Override
    public ShardEntryGroup getWriteShardGroup( final ApplicationScope scope, final long timestamp,
                                               final DirectedEdgeMeta directedEdgeMeta ) {

        ValidationUtils.validateApplicationScope(scope);
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final CacheKey key = new CacheKey( scope, directedEdgeMeta );
        CacheEntry entry;

        try {
            entry = this.graphs.get( key );
        }
        catch ( ExecutionException e ) {
            throw new GraphRuntimeException( "Unable to load shard key for graph", e );
        }

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
        ValidationUtils.validateApplicationScope(scope);
        GraphValidation.validateDirectedEdgeMeta( directedEdgeMeta );

        final CacheKey key = new CacheKey( scope, directedEdgeMeta );
        CacheEntry entry;

        try {
            entry = this.graphs.get( key );
        }
        catch ( ExecutionException e ) {
            throw new GraphRuntimeException( "Unable to load shard key for graph", e );
        }

        Iterator<ShardEntryGroup> iterator = entry.getShards( maxTimestamp );

        if ( iterator == null ) {
            return Collections.<ShardEntryGroup>emptyList().iterator();
        }

        return iterator;
    }


    /**
     * This is a race condition.  We could re-init the shard while another thread is reading it.  This is fine, the read
     * doesn't have to be precise.  The algorithm accounts for stale data.
     */
    private void updateCache() {

        this.graphs = CacheBuilder.newBuilder().expireAfterWrite( graphFig.getShardCacheSize(), TimeUnit.MILLISECONDS )
                                  .removalListener( new ShardRemovalListener() )
                                  .maximumWeight( MAX_WEIGHT_PER_ELEMENT * graphFig.getShardCacheSize() )
                                  .weigher( new ShardWeigher() ).build( new ShardCacheLoader() );
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


    /**
     * An entry for the shard.
     */
    public static final class CacheEntry {
        /**
         * Get the list of all segments
         */
        private TreeMap<Long, ShardEntryGroup> shards;


        private CacheEntry( final Iterator<ShardEntryGroup> shards ) {
            Preconditions.checkArgument( shards.hasNext(),
                    "More than 1 entry must be present in the shard to load into cache" );

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
         * Return the size of the elements in the cache
         */
        public int getCacheSize() {
            return this.shards.size();
        }


        /**
         * Get all shards <= this one in decending order
         */
        public Iterator<ShardEntryGroup> getShards( final Long maxShard ) {

            final Long firstKey = shards.floorKey( maxShard );

            return shards.headMap( firstKey, true ).descendingMap().values().iterator();
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


    /**
     * Load the cache entries from the shards we have stored
     */
    final class ShardCacheLoader extends CacheLoader<CacheKey, CacheEntry> {


        @Override
        public CacheEntry load( final CacheKey key ) throws Exception {


            final Iterator<ShardEntryGroup> edges =
                    nodeShardAllocation.getShards( key.scope, Optional.<Shard>absent(), key.directedEdgeMeta );

            return new CacheEntry( edges );
        }
    }


    /**
     * Calculates the weight of the entry by geting the size of the cache
     */
    final class ShardWeigher implements Weigher<CacheKey, CacheEntry> {

        @Override
        public int weigh( final CacheKey key, final CacheEntry value ) {
            return value.getCacheSize();
        }
    }


    /**
     * On removal from the cache, we want to audit the maximum shard.  If it needs to allocate a new shard, we want to
     * do so. IF there's a compaction pending, we want to run the compaction task
     */
    final class ShardRemovalListener implements RemovalListener<CacheKey, CacheEntry> {

        @Override
        public void onRemoval( final RemovalNotification<CacheKey, CacheEntry> notification ) {


            final CacheKey key = notification.getKey();
            final CacheEntry entry = notification.getValue();


            Iterator<ShardEntryGroup> groups = entry.getShards( Long.MAX_VALUE );


            /**
             * Start at our max, then
             */

            //audit all our groups
            while ( groups.hasNext() ) {
                ShardEntryGroup group = groups.next();

                /**
                 * We don't have a compaction pending.  Run an audit on the shards
                 */
                if ( !group.isCompactionPending() ) {
                    LOG.debug( "No compaction pending, checking max shard on expiration" );
                    /**
                     * Check if we should allocate, we may want to
                     */


                    nodeShardAllocation.auditShard( key.scope, group, key.directedEdgeMeta );
                    continue;
                }
                /**
                 * Do the compaction
                 */
                if ( group.shouldCompact( timeservice.getCurrentTime() ) ) {
                    //launch the compaction
                }

                //no op, there's nothing we need to do to this shard

            }
        }
    }
}
