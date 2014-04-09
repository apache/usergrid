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


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSeriesCounterSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.astyanax.serializers.UUIDSerializer;


/**
 * Implementation for doing approximation.  Uses hy perlog log.
 *
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/stream/cardinality
 * /HyperLogLog.java
 */


public class NodeShardApproximationImpl implements NodeShardApproximation {

    //TODO T.N. refactor into an expiring local cache.  We need each hyperlog to be it's own instance of a given shard
    //if this is to work\
    private static final UUIDSerializer UUID_SERIALIZER = UUIDSerializer.get();

    /**
     * We generate a new time uuid every time a new instance is started.  We should never re-use an instance.
     */
    private final UUID identity = UUIDGenerator.newTimeUUID();

    private final GraphFig graphFig;

    private final LoadingCache<ShardKey, HyperLogLog> graphLogs;

    private final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization;


    /**
     * Create a time shard approximation with the correct configuration.
     */
    @Inject
    public NodeShardApproximationImpl( final GraphFig graphFig,
                                       final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization ) {
        this.graphFig = graphFig;
        this.edgeSeriesCounterSerialization = edgeSeriesCounterSerialization;

        graphLogs = CacheBuilder.newBuilder()
               .maximumSize( graphFig.getShardCacheTimeout() )
               .build( new CacheLoader<ShardKey, HyperLogLog>() {
                   public HyperLogLog load( ShardKey key ) {
                       return loadCache( key );
                   }
               } );

    }


    @Override
    public void increment( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                           final String... edgeType ) {



        final ByteBuffer buff = UUID_SERIALIZER.toByteBuffer( shardId );

        byte[] bytes = buff.array();



        long longHash = MurmurHash.hash64(bytes, bytes.length );

        final ShardKey key = new ShardKey( scope, nodeId, shardId, edgeType );

        try {
            graphLogs.get( key).offerHashed( longHash );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to get hyperloglog from cache", e );
        }


    }


    private HyperLogLog loadCache(ShardKey key){
//         edgeSeriesCounterSerialization
        return null;
    }


    @Override
    public long getCount( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                          final String... edgeType ) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private byte[] hash( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                         final String... edgeTypes ) {
        StringBuilder builder = new StringBuilder();

        final Id organization = scope.getOrganization();

        builder.append( organization.getUuid() );
        builder.append( organization.getType() );

        builder.append( nodeId.getUuid() );
        builder.append( nodeId.getType() );

        builder.append( shardId.toString() );

        for ( String edgeType : edgeTypes ) {
            builder.append( edgeType );
        }

        return null;
//        return builder.toString().getBytes( CHARSET );
    }


    /**
     * Internal class for shard keys
     */
    private static final class ShardKey {
        private final OrganizationScope scope;
        private final Id nodeId;
        private final UUID shardId;
        private final String[] edgeTypes;


        private ShardKey( final OrganizationScope scope, final Id nodeId, final UUID shardId, final String[] edgeTypes ) {


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

            if ( !Arrays.equals( edgeTypes, shardKey.edgeTypes ) ) {
                return false;
            }
            if ( !nodeId.equals( shardKey.nodeId ) ) {
                return false;
            }
            if ( !scope.equals( shardKey.scope ) ) {
                return false;
            }
            if ( !shardId.equals( shardKey.shardId ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            int result = scope.hashCode();
            result = 31 * result + nodeId.hashCode();
            result = 31 * result + shardId.hashCode();
            result = 31 * result + Arrays.hashCode( edgeTypes );
            return result;
        }
    }
}
