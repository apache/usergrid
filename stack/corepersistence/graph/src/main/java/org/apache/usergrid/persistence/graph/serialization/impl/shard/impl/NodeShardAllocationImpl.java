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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.iterators.PushbackIterator;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.consistency.TimeService;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeSeriesCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeSeriesSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Implementation of the node shard monitor and allocation
 */
public class NodeShardAllocationImpl implements NodeShardAllocation {


    private final EdgeSeriesSerialization edgeSeriesSerialization;
    private final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization;
    private final TimeService timeService;
    private final GraphFig graphFig;
    private final Keyspace keyspace;


    @Inject
    public NodeShardAllocationImpl( final EdgeSeriesSerialization edgeSeriesSerialization,
                                    final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization,
                                    final TimeService timeService, final GraphFig graphFig, final Keyspace keyspace ) {
        this.edgeSeriesSerialization = edgeSeriesSerialization;
        this.edgeSeriesCounterSerialization = edgeSeriesCounterSerialization;
        this.timeService = timeService;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
    }


    @Override
    public Iterator<Long> getShards( final OrganizationScope scope, final Id nodeId, final long maxShardId,
                                     final int pageSize, final String... edgeTypes ) {

        final Iterator<Long> existingShards =
                edgeSeriesSerialization.getEdgeMetaData( scope, nodeId, maxShardId, pageSize, edgeTypes );

        final PushbackIterator<Long> pushbackIterator = new PushbackIterator( existingShards );


        final long now = timeService.getCurrentTime();


        final List<Long> futures = new ArrayList<Long>();


        //loop through all shards, any shard > now+1 should be deleted
        while ( pushbackIterator.hasNext() ) {

            final Long value = pushbackIterator.next();

            //we're done, our current time uuid is greater than the value stored
            if ( now >= value ) {
                //push it back into the iterator
                pushbackIterator.pushback( value );
                break;
            }

            futures.add( value );
        }


        //we have more than 1 future value, we need to remove it

        MutationBatch cleanup = keyspace.prepareMutationBatch();

        //remove all futures except the last one, it is the only value we shouldn't lazy remove
        for ( int i = 0; i < futures.size() -1; i++ ) {
            final long toRemove = futures.get( i );

            final MutationBatch batch = edgeSeriesSerialization.removeEdgeMeta( scope, nodeId, toRemove, edgeTypes );

            cleanup.mergeShallow( batch );
        }


        try {
            cleanup.execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to remove future shards, mutation error", e );
        }


        final int futuresSize =  futures.size();

        if ( futuresSize > 0 ) {
            pushbackIterator.pushback( futures.get( futuresSize - 1 ) );
        }


        return pushbackIterator;
    }


    @Override
    public boolean auditMaxShard( final OrganizationScope scope, final Id nodeId, final String... edgeType ) {

        final long now =  timeService.getCurrentTime() ;

        final Iterator<Long> maxShards = getShards( scope, nodeId, Long.MAX_VALUE, 1, edgeType );


        //if the first shard has already been allocated, do nothing.

        //now is already > than the max, don't do anything
        if ( !maxShards.hasNext() ) {
            return false;
        }

        final long maxShard = maxShards.next();

        /**
         * Nothing to do, it's already in the future
         */
        if ( maxShard > now ) {
            return false;
        }


        /**
         * Check out if we have a count for our shard allocation
         */
        final long count = edgeSeriesCounterSerialization.getCount( scope, nodeId, maxShard, edgeType );

        if ( count < graphFig.getShardSize() ) {
            return false;
        }

        //try to get a lock here, and fail if one isn't present

        final long newShardTime = timeService.getCurrentTime() + graphFig.getShardCacheTimeout() * 2;


        try {
            this.edgeSeriesSerialization.writeEdgeMeta( scope, nodeId, newShardTime, edgeType ).execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to write the new edge metadata" );
        }


        return true;
    }
}
