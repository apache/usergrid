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
package org.apache.usergrid.persistence.graph.impl.cache;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSeriesSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Simple implementation of the cache.  Uses a local Guava cache with a timeout.  If a value is not present in the
 * cache, it will need to be searched via cassandra.
 */
public class NodeShardCacheImpl implements NodeShardCache {

    /**
     * The minimum uuid we allocate
     */
    private static final UUID MIN_UUID =  new UUID( 0, 1 );

    private final EdgeSeriesSerialization edgeSeriesSerialization;
    private final GraphFig graphFig;

    private LoadingCache<CacheKey, CacheEntry> graphs;


    /**
     *
     * @param edgeSeriesSerialization
     * @param graphFig
     */
    @Inject
    public NodeShardCacheImpl( final EdgeSeriesSerialization edgeSeriesSerialization, final GraphFig graphFig ) {
        this.edgeSeriesSerialization = edgeSeriesSerialization;
        this.graphFig = graphFig;

        /**
         * Add our listener to reconstruct the cache
         */
        this.graphFig.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                final String propertyName = evt.getPropertyName();

                if ( propertyName.equals( GraphFig.CACHE_SIZE ) || propertyName.equals( GraphFig.CACHE_TIMEOUT ) ) {
                    updateCache();
                }
            }
        } );

        /**
         * Initialize the cache
         */
        updateCache();
    }


    @Override
    public UUID getSlice( final OrganizationScope scope, final Id nodeId, final UUID time, final String... edgeType ) {


        final CacheKey key = new CacheKey(scope, nodeId, edgeType  );
        CacheEntry entry;

        try {
            entry = this.graphs.get( key );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to load cache key for graph", e );
        }

        List<UUID> shards = entry.getShardId(time, 1 );

        if(shards == null || shards.size() == 0){

            try {
                edgeSeriesSerialization.writeEdgeMeta( scope, nodeId, MIN_UUID, edgeType ).execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException("Unable to write edge meta data", e);
            }

            return MIN_UUID;
         }

        final UUID shardId = entry.getShardId(time, 1).get( 0 );

        if(shardId != null){
            return shardId;
        }

        //if we get here, something went wrong, our cache should always have a min time UUID.
        throw new RuntimeException( "No time UUID shard was found and could not allocate one" );

    }



    /**
     * This is a race condition.  We could re-init the cache while another thread is reading it.  This is fine, the read
     * doesn't have to be precise.  The algorithm accounts for stale data.
     */
    private void updateCache() {

        this.graphs = CacheBuilder.newBuilder().maximumSize( graphFig.getCacheSize() )
                                  .expireAfterWrite( graphFig.getCacheTimeout(), TimeUnit.MILLISECONDS )
                                  .build( new CacheLoader<CacheKey, CacheEntry>() {


                                      @Override
                                      public CacheEntry load( final CacheKey key ) throws Exception {
                                          final List<UUID> edges = edgeSeriesSerialization
                                                  .getEdgeMetaData( key.scope, key.id, key.types );

                                          return new CacheEntry( edges );
                                      }
                                  } );
    }



    /**
     * Cache key for looking up items in the cache
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
     * An entry for the cache.
     */
    private static class CacheEntry {
        /**
         * Get the list of all segments
         */
        private List<UUID> shards;


        private CacheEntry( final List<UUID> shards ) {
            this.shards = shards;
        }


        /**
         * Get the shard's UUID for the uuid we're attempting to seek from
         * @param seek
         * @return
         */
        public List<UUID> getShardId( final UUID seek, final int size) {
            int index = Collections.binarySearch( this.shards, seek, MetaComparator.INSTANCE );

            /**
             * We have an exact match, return it
             */
            if(index > -1){
               return getShards(index, size);
            }


            //update the index to represent the index we should insert to and read it.  This will be <= the UUID we were passed
            index = index*-1+1;


            if(index < shards.size()){
                return getShards( index, size );
            }


            return null;
        }

        /**
          * Get the ordered list of shards from high to low
          * @param index
          * @param size
          * @return
          */
         private List<UUID> getShards(final int index, final int size){
             int toIndex = Math.min( shards.size(), index+size );
             return shards.subList(index,  toIndex );
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
