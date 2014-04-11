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
package org.apache.usergrid.persistence.graph.impl.shard;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.util.IterableUtil;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

import static org.apache.usergrid.persistence.graph.impl.Constants.MAX_UUID;


/**
 * Simple implementation of the shard.  Uses a local Guava shard with a timeout.  If a value is not present in the
 * shard, it will need to be searched via cassandra.
 */
public class NodeShardCacheImpl implements NodeShardCache {


    private static final int SHARD_PAGE_SIZE = 1000;


    private final NodeShardAllocation nodeShardAllocation;
    private final GraphFig graphFig;

    private LoadingCache<CacheKey, CacheEntry> graphs;


    /**
     *
     * @param nodeShardAllocation
     * @param graphFig
     */
    @Inject
    public NodeShardCacheImpl( final NodeShardAllocation nodeShardAllocation, final GraphFig graphFig ) {
        this.nodeShardAllocation = nodeShardAllocation;
        this.graphFig = graphFig;

        /**
         * Add our listener to reconstruct the shard
         */
        this.graphFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propertyName = evt.getPropertyName();

                if ( propertyName.equals( GraphFig.SHARD_CACHE_SIZE ) || propertyName.equals( GraphFig.SHARD_CACHE_TIMEOUT ) ) {
                    updateCache();
                }
            }
        } );

        /**
         * Initialize the shard
         */
        updateCache();
    }


    @Override
    public UUID getSlice( final OrganizationScope scope, final Id nodeId, final UUID time, final String... edgeType ) {


        final CacheKey key = new CacheKey( scope, nodeId, edgeType );
        CacheEntry entry;

        try {
            entry = this.graphs.get( key );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to load shard key for graph", e );
        }

        final UUID shardId = entry.getShardId( time );

        if ( shardId != null ) {
            return shardId;
        }

        //if we get here, something went wrong, our shard should always have a time UUID to return to us
        throw new RuntimeException( "No time UUID shard was found and could not allocate one" );
    }


    /**
     * This is a race condition.  We could re-init the shard while another thread is reading it.  This is fine, the read
     * doesn't have to be precise.  The algorithm accounts for stale data.
     */
    private void updateCache() {

        this.graphs = CacheBuilder.newBuilder().maximumSize( graphFig.getShardCacheSize() )
                                  .expireAfterWrite( graphFig.getShardCacheSize(), TimeUnit.MILLISECONDS )
                                  .build( new CacheLoader<CacheKey, CacheEntry>() {


                                      @Override
                                      public CacheEntry load( final CacheKey key ) throws Exception {


                                          /**
                                           * Perform an audit in case we need to allocate a new shard
                                           */
                                          nodeShardAllocation.auditMaxShard( key.scope, key.id, key.types );
                                             //TODO, we need to put some sort of upper bounds on this, it could possibly
                                          //get too large




                                          final Iterator<UUID> edges = nodeShardAllocation
                                                                                          .getShards( key.scope, key.id, MAX_UUID, SHARD_PAGE_SIZE, key.types );

                                          return new CacheEntry( edges );
                                      }
                                  } );
    }


    /**
     * Cache key for looking up items in the shard
     */
    private static class CacheKey {
        private final OrganizationScope scope;
        private final Id id;
        private final String[] types;


        private CacheKey( final OrganizationScope scope, final Id id, final String[] types ) {
            this.scope = scope;
            this.id = id;
            this.types = types;
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

            if ( !id.equals( cacheKey.id ) ) {
                return false;
            }
            if ( !Arrays.equals( types, cacheKey.types ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + Arrays.hashCode( types );
            return result;
        }
    }


    /**
     * An entry for the shard.
     */
    private static class CacheEntry {
        /**
         * Get the list of all segments
         */
        private TreeSet<UUID> shards;


        private CacheEntry( final Iterator<UUID> shards ) {
            this.shards = new TreeSet<>( MetaComparator.INSTANCE );

            for ( UUID shard : IterableUtil.wrap( shards ) ) {
                this.shards.add( shard );
            }
        }


        /**
         * Get the shard's UUID for the uuid we're attempting to seek from
         */
        public UUID getShardId( final UUID seek ) {
            return this.shards.floor( seek );
        }
    }


    /**
     * UUID Comparator
     */
    private static class MetaComparator implements Comparator<UUID> {

        public static final UUIDComparator INSTANCE = new UUIDComparator();


        @Override
        public int compare( final UUID o1, final UUID o2 ) {
            return com.fasterxml.uuid.UUIDComparator.staticCompare( o1, o2 );
        }
    }
}
