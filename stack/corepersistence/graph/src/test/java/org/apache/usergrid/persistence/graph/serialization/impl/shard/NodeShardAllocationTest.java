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
package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.count.NodeShardCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.count.ShardKey;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardAllocationImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;

import static junit.framework.TestCase.assertTrue;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NodeShardAllocationTest {


    private GraphFig graphFig;


    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );

        graphFig = mock( GraphFig.class );

        when( graphFig.getShardCacheSize() ).thenReturn( 10000l );
        when( graphFig.getShardSize() ).thenReturn( 20000l );
        when( graphFig.getShardCacheTimeout()).thenReturn( 30000l );
    }


    @Test
    public void noShards() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardCounterSerialization =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch batch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn( batch );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardCounterSerialization, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";

        /**
         * Mock up returning an empty iterator, our audit shouldn't create a new shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ),  same( type ),
                        same( subType ) ) ).thenReturn( Collections.<Long>emptyList().iterator() );

        final boolean result = approximation.auditMaxShard( scope, nodeId, type, subType );

        assertFalse( "No shard allocated", result );
    }


    @Test
    public void existingFutureShard() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardCounterSerialization =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );


        final MutationBatch batch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn( batch );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardCounterSerialization, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final long futureShard =  timeservicetime + graphFig.getShardCacheTimeout() * 2 ;

        /**
         * Mock up returning a min shard, and a future shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ),  same( type ),
                        same( subType ) ) ).thenReturn( Arrays.asList( futureShard ).iterator() );

        final boolean result = approximation.auditMaxShard( scope, nodeId, type, subType );

        assertFalse( "No shard allocated", result );
    }


    @Test
    public void lowCountFutureShard() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardApproximation =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch batch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn( batch );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardApproximation, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );


        /**
         * Mock up returning a min shard, and a future shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ),  same( type ),
                        same( subType ) ) ).thenReturn( Arrays.asList( 0l ).iterator() );


        //return a shard size < our max by 1

        final long count = graphFig.getShardSize() - 1;

        when( nodeShardApproximation.getCount(scope, nodeId, 0l, type, subType ))
                                           .thenReturn( count );

        final boolean result = approximation.auditMaxShard( scope, nodeId, type, subType );

        assertFalse( "Shard allocated", result );
    }


    @Test
    public void equalCountFutureShard() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardApproximation =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch batch = mock(MutationBatch.class);

        when(keyspace.prepareMutationBatch()).thenReturn( batch );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardApproximation, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );


        /**
         * Mock up returning a min shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ),  same( type ),
                        same( subType ) ) ).thenReturn( Arrays.asList( 0l ).iterator() );


        final long shardCount = graphFig.getShardSize();

        //return a shard size equal to our max
        when( nodeShardApproximation
                .getCount(   scope , nodeId, 0l,type , subType  ))
                .thenReturn( shardCount );

        ArgumentCaptor<Long> newUUIDValue = ArgumentCaptor.forClass( Long.class );


        //mock up our mutation
        when( edgeShardSerialization
                .writeEdgeMeta( same( scope ), same( nodeId ), newUUIDValue.capture(), same( type ), same( subType ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final boolean result = approximation.auditMaxShard( scope, nodeId, type, subType );

        assertTrue( "Shard allocated", result );

        //check our new allocated UUID

        final long expectedTime = timeservicetime + 2 * graphFig.getShardCacheTimeout();

        final long savedTimestamp = newUUIDValue.getValue();



        assertEquals( "Expected at 2x timeout generated", expectedTime, savedTimestamp );
    }




    @Test
    public void futureCountShardCleanup() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardApproximation =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch batch = mock(MutationBatch.class);

        when(keyspace.prepareMutationBatch()).thenReturn( batch );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardApproximation, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";


        /**
         * Use the time service to generate UUIDS
         */
        final long timeservicetime = System.currentTimeMillis();


        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        assertTrue("Shard cache mocked", graphFig.getShardCacheTimeout() > 0);


        /**
         * Simulates clock drift when 2 nodes create future shards near one another
         */
        final long futureTime = timeService.getCurrentTime()  + 2 * graphFig.getShardCacheTimeout();


        /**
         * Simulate slow node
         */
        final long futureShard1 = futureTime - 1;

        final long futureShard2 = futureTime + 10000;

        final long futureShard3 = futureShard2 + 10000;


        final int pageSize = 100;

        /**
         * Mock up returning a min shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ), same( type ),
                        same( subType ) ) ).thenReturn( Arrays.asList(futureShard3, futureShard2, futureShard1, 0l).iterator() );



        ArgumentCaptor<Long> newLongValue = ArgumentCaptor.forClass( Long.class );




        //mock up our mutation
        when( edgeShardSerialization
                .removeEdgeMeta( same( scope ), same( nodeId ), newLongValue.capture(), same( type ), same( subType ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final Iterator<Long>
                result = approximation.getShards( scope, nodeId, Optional.<Long>absent(), type, subType );


        assertTrue( "Shards present", result.hasNext() );

        assertEquals("Only single next shard returned", futureShard1,  result.next().longValue());

        assertTrue("Shards present", result.hasNext());

        assertEquals("Previous shard present", 0l, result.next().longValue());

        assertFalse("No shards left", result.hasNext());

        /**
         * Now we need to verify that both our mutations have been added
         */

        List<Long> values = newLongValue.getAllValues();

        assertEquals("2 values removed", 2,  values.size());

        assertEquals("Deleted Max Future", futureShard3, values.get( 0 ).longValue());
        assertEquals("Deleted Next Future", futureShard2, values.get( 1 ).longValue());

    }




    @Test
    public void noShardsReturns() {
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final NodeShardApproximation nodeShardApproximation =
                mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final MutationBatch batch = mock( MutationBatch.class );

        when( keyspace.prepareMutationBatch() ).thenReturn( batch );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, nodeShardApproximation, timeService,
                        graphFig );

        final Id nodeId = createId( "test" );
        final String type = "type";
        final String subType = "subType";

        /**
         * Mock up returning an empty iterator, our audit shouldn't create a new shard
         */
        when( edgeShardSerialization
                .getEdgeMetaData( same( scope ), same( nodeId ), any( Optional.class ),  same( type ),
                        same( subType ) ) ).thenReturn( Collections.<Long>emptyList().iterator() );

        final Iterator<Long> result = approximation.getShards( scope, nodeId, Optional.<Long>absent(), type, subType );

        assertEquals("0 shard allocated", 0l, result.next().longValue());

        assertFalse( "No shard allocated", result.hasNext() );
    }

}
