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


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
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
    private final GraphFig graphFig;


    @Inject
    public NodeShardAllocationImpl( final EdgeSeriesSerialization edgeSeriesSerialization,
                                    final EdgeSeriesCounterSerialization edgeSeriesCounterSerialization,
                                    final GraphFig graphFig ) {
        this.edgeSeriesSerialization = edgeSeriesSerialization;
        this.edgeSeriesCounterSerialization = edgeSeriesCounterSerialization;
        this.graphFig = graphFig;
    }


    @Override
    public List<UUID> getShards( final OrganizationScope scope, final Id nodeId, final UUID maxShardId, final int count,
                                 final String... edgeTypes ) {
        return edgeSeriesSerialization.getEdgeMetaData( scope, nodeId, maxShardId, count, edgeTypes );
    }


    @Override
    public boolean auditMaxShard( final OrganizationScope scope, final Id nodeId, final String... edgeType ) {

        final UUID now = UUIDGenerator.newTimeUUID();

        List<UUID> maxShards = getShards( scope, nodeId, MAX_UUID, 1, edgeType );

        //if the first shard has already been allocated, do nothing.

        //now is already > than the max, don't do anything
        if ( maxShards.size() > 0 && UUIDComparator.staticCompare( now, maxShards.get( 0 ) ) < 0 ) {
            return false;
        }

        //allocate a new shard
        //TODO T.N. modify the time uuid utils to allow future generation, this is incorrect, but a place holder
        final UUID futureUUID = UUIDGenerator.newTimeUUID();

        try {
            this.edgeSeriesSerialization.writeEdgeMeta( scope, nodeId, futureUUID, edgeType ).execute();
        }
        catch ( ConnectionException e ) {
            throw new RuntimeException( "Unable to write the new edge metadata" );
        }

        UUID max = null;


        MutationBatch rollup = null;
        boolean completed = false;

        //TODO, change this into an iterator
        while ( !completed ) {

            List<UUID> shards = getShards( scope, nodeId, MAX_UUID, 100, edgeType );

            for ( UUID shardId : shards ) {
                if ( UUIDComparator.staticCompare( shardId, max ) >= 0 ) {
                    completed = true;
                    break;
                }


                final MutationBatch batch = edgeSeriesSerialization.removeEdgeMeta( scope, nodeId, shardId, edgeType );

                if ( rollup == null ) {
                    rollup = batch;
                }
                else {
                    rollup.mergeShallow( batch );
                }
            }


            //while our max value is > than the value we just created, delete it
        }

        if(rollup != null){
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
