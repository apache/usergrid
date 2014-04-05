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


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.consistency.TimeService;
import org.apache.usergrid.persistence.graph.serialization.EdgeSeriesCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSeriesSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.impl.Constants.MAX_UUID;


/**
 * Implementation of the node shard monitor and allocation
 */
public class NodeShardAllocationImpl implements NodeShardAllocation {


    private final EdgeSeriesSerialization edgeSeriesSerialization;
    private final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization;
    private final TimeService timeService;
    private final GraphFig graphFig;


    @Inject
    public NodeShardAllocationImpl( final EdgeSeriesSerialization edgeSeriesSerialization,
                                    final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization,
                                    final TimeService timeService, final GraphFig graphFig ) {
        this.edgeSeriesSerialization = edgeSeriesSerialization;
        this.edgeSeriesCounterSerialization = edgeSeriesCounterSerialization;
        this.timeService = timeService;
        this.graphFig = graphFig;
    }


    @Override
    public Iterator<UUID> getShards( final OrganizationScope scope, final Id nodeId, final UUID maxShardId,
                                     final int pageSize, final String... edgeTypes ) {
        return edgeSeriesSerialization.getEdgeMetaData( scope, nodeId, maxShardId, pageSize, edgeTypes );
    }


    @Override
    public boolean auditMaxShard( final OrganizationScope scope, final Id nodeId, final String... edgeType ) {

        final UUID now = UUIDGenerator.newTimeUUID();

        Iterator<UUID> maxShards = getShards( scope, nodeId, MAX_UUID, 1, edgeType );

        //if the first shard has already been allocated, do nothing.

        //now is already > than the max, don't do anything
        if ( maxShards.hasNext() && UUIDComparator.staticCompare( now, maxShards.next() ) < 0 ) {
            return false;
        }

        final long newShardTime = timeService.getCurrentTime() + graphFig.getCacheTimeout()*2;

        //allocate a new shard at least now+ 2x our shard timeout.  We want to be sure that all replicas pick up on the new
        //shard

        final UUID futureUUID = UUIDGenerator.newTimeUUID(newShardTime);

        try {
            this.edgeSeriesSerialization.writeEdgeMeta( scope, nodeId, futureUUID, edgeType ).execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to write the new edge metadata" );
        }

        UUID max = null;


        MutationBatch rollup = null;

        Iterator<UUID> shards = getShards( scope, nodeId, MAX_UUID, 1000, edgeType );

        while ( shards.hasNext() ) {

            final UUID shardId = shards.next();

            if ( UUIDComparator.staticCompare( shardId, max ) >= 0 ) {
                break;
            }


            //remove the edge that is too large from the node shard allocation
            final MutationBatch batch = edgeSeriesSerialization.removeEdgeMeta( scope, nodeId, shardId, edgeType );

            if ( rollup == null ) {
                rollup = batch;
            }
            else {
                rollup.mergeShallow( batch );
            }


            //while our max value is > than the value we just created, delete it
        }

        if ( rollup != null ) {
            try {
                rollup.execute();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to cleanup allocated shards" );
            }
        }

        return true;
    }


    @Override
    public void increment( final OrganizationScope scope, final Id nodeId, final UUID shardId, final int count,
                           final String... edgeType ) {
        //delegate
        edgeSeriesCounterSerialization.incrementMetadataCount( scope, nodeId, shardId, count, edgeType );
    }
}
