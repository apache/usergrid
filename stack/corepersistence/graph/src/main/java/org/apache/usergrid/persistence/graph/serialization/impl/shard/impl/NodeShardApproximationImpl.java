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


import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


/**
 * Implementation for doing edge approximation based on counters.  Uses a guava loading cache to load values from
 * cassandra, and flush them on cache eviction.
 *
 */
public class NodeShardApproximationImpl implements NodeShardApproximation {

       private final LoadingCache<ShardKey, AtomicLong> graphLogs;


    /**
     * Create a time shard approximation with the correct configuration.
     */
    @Inject
    public NodeShardApproximationImpl( final GraphFig graphFig) {

        graphLogs = CacheBuilder.newBuilder()
               .maximumSize( graphFig.getShardCacheSize() )
               .build( new CacheLoader<ShardKey, AtomicLong>() {
                   public AtomicLong load( ShardKey key ) {
                       return new AtomicLong(  );
                   }
               } );

    }


    @Override
    public void increment( final ApplicationScope scope, final Id nodeId, final long shardId,  final long count,
                           final String... edgeType ) {


        final ShardKey key = new ShardKey( scope, nodeId, shardId, edgeType );

        try {
            graphLogs.get( key).addAndGet(count);
        }
        catch ( ExecutionException e ) {
            throw new GraphRuntimeException( "Unable to get hyperloglog from cache", e );
        }


    }


    @Override
    public long getCount( final ApplicationScope scope, final Id nodeId, final long shardId,
                          final String... edgeType ) {

        final ShardKey key = new ShardKey( scope, nodeId, shardId, edgeType );


        try {
            return graphLogs.get( key ).get();
        }
        catch ( ExecutionException e ) {
            throw new GraphRuntimeException("Unable to execute cache get", e);
        }
    }



    /**
     * Internal class for shard keys
     */
    private static final class ShardKey {
        private final ApplicationScope scope;
        private final Id nodeId;
        private final long shardId;
        private final String[] edgeTypes;


        private ShardKey( final ApplicationScope scope, final Id nodeId, final long shardId, final String[] edgeTypes ) {


            this.scope = scope;
            this.nodeId = nodeId;
            this.shardId = shardId;
            this.edgeTypes = edgeTypes;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( o == null || getClass() != o.getClass() ) {
                return false;
            }

            final ShardKey shardKey = ( ShardKey ) o;

            if ( shardId != shardKey.shardId ) {
                return false;
            }
            if ( !Arrays.equals( edgeTypes, shardKey.edgeTypes ) ) {
                return false;
            }
            if ( !nodeId.equals( shardKey.nodeId ) ) {
                return false;
            }
            if ( !scope.equals( shardKey.scope ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = scope.hashCode();
            result = 31 * result + nodeId.hashCode();
            result = 31 * result + ( int ) ( shardId ^ ( shardId >>> 32 ) );
            result = 31 * result + Arrays.hashCode( edgeTypes );
            return result;
        }
    }
}
