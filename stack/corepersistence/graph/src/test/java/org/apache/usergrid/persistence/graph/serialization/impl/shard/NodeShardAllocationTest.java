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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.exception.GraphRuntimeException;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardAllocationImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static junit.framework.TestCase.assertTrue;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
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

        final long timeout = 30000;
        when( graphFig.getShardCacheTimeout() ).thenReturn( timeout );
        when( graphFig.getShardMinDelta() ).thenReturn( timeout * 2 );
    }


    @Test
    public void minTime() {
        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardCounterSerialization = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardCounterSerialization, timeService, graphFig, shardGroupCompaction );


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final long expected = timeservicetime - 2 * graphFig.getShardCacheTimeout();

        final long returned = approximation.getMinTime();

        assertEquals( "Correct time was returned", expected, returned );
    }


    @Test
    public void existingFutureShardSameTime() {
        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardCounterSerialization = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardCounterSerialization, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final Shard firstShard = new Shard( 0l, 0l, true );
        final Shard futureShard = new Shard( 10000l, timeservicetime, false );

        final ShardEntryGroup shardEntryGroup = new ShardEntryGroup( 1000l );
        shardEntryGroup.addShard( futureShard );
        shardEntryGroup.addShard( firstShard );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( nodeId, type, subType );

        final boolean result = approximation.auditShard( scope, shardEntryGroup, targetEdgeMeta );

        assertFalse( "No shard allocated", result );
    }


    @Test
    public void lowCountFutureShard() {
        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final Shard futureShard = new Shard( 10000l, timeservicetime, true );

        final ShardEntryGroup shardEntryGroup = new ShardEntryGroup( 1000l );
        shardEntryGroup.addShard( futureShard );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( nodeId, type, subType );


        //return a shard size < our max by 1

        final long count = graphFig.getShardSize() - 1;

        when( nodeShardApproximation.getCount( scope, futureShard, targetEdgeMeta ) ).thenReturn( count );

        final boolean result = approximation.auditShard( scope, shardEntryGroup, targetEdgeMeta );

        assertFalse( "Shard allocated", result );
    }


    @Test
    public void overAllocatedShard() {
        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final Shard futureShard = new Shard( 0l, 0l, true );

        final ShardEntryGroup shardEntryGroup = new ShardEntryGroup( 1000l );
        shardEntryGroup.addShard( futureShard );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( nodeId, type, subType );


        /**
         * Allocate 2.5x what this shard should have.  We should ultimately have a split at 2x
         */
        final long shardCount = ( long ) ( graphFig.getShardSize() * 2.5 );


        //return a shard size equal to our max
        when( nodeShardApproximation.getCount( scope, futureShard, targetEdgeMeta ) ).thenReturn( shardCount );


        //this is how many we should be iterating and should set the value of the last shard we keep
        final int numToIterate = ( int ) ( graphFig.getShardSize() * 2 );


        /**
         * Just use 2 edges.  It means that we won't generate a boatload of data and kill our test. We just want
         * to check that the one we want to return is correct
         */
        SimpleMarkedEdge skipped = new SimpleMarkedEdge( nodeId, type, IdGenerator.createId( subType ), 10000, false );
        SimpleMarkedEdge keep = new SimpleMarkedEdge( nodeId, type, IdGenerator.createId( subType ), 20000, false );

        //allocate some extra to ensure we seek the right value
        List<MarkedEdge> edges = new ArrayList( numToIterate + 100 );

        int i = 0;

        for (; i < numToIterate - 1; i++ ) {
            edges.add( skipped );
        }

        //add our keep edge
        edges.add( keep );
        i++;

        for (; i < shardCount; i++ ) {

            edges.add( skipped );
        }


        final Iterator<MarkedEdge> edgeIterator = edges.iterator();

        //mock up returning the value
        when( shardedEdgeSerialization
                .getEdgesFromSourceByTargetType( same( edgeColumnFamilies ), same( scope ), any( SearchByIdType.class ),
                        any( Collection.class ) ) ).thenReturn( edgeIterator );


        /**
         * Mock up the write shard meta data
         */
        ArgumentCaptor<Shard> shardValue = ArgumentCaptor.forClass( Shard.class );


        //mock up our mutation
        when( edgeShardSerialization.writeShardMeta( same( scope ), shardValue.capture(), same( targetEdgeMeta ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final boolean result = approximation.auditShard( scope, shardEntryGroup, targetEdgeMeta );

        assertTrue( "Shard was split correctly", result );

        final long savedTimestamp = shardValue.getValue().getCreatedTime();


        assertEquals( "Expected time service time", timeservicetime, savedTimestamp );


        //now check our max value was set.  Since our shard is significantly over allocated, we should be iterating
        //through elements to move the pivot down to a more manageable size

        final long savedShardPivot = shardValue.getValue().getShardIndex();

        assertEquals( "Expected max value to be the same", keep.getTimestamp(), savedShardPivot );
    }


    @Test
    public void equalCountFutureShard() {

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final Shard futureShard = new Shard( 0l, 0l, true );

        final ShardEntryGroup shardEntryGroup = new ShardEntryGroup( 1000l );
        shardEntryGroup.addShard( futureShard );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( nodeId, type, subType );

        final long shardCount = graphFig.getShardSize();


        final SimpleMarkedEdge skippedEdge = new SimpleMarkedEdge( nodeId, type, IdGenerator.createId( "subType" ), 10000l, false );
        final SimpleMarkedEdge returnedEdge =
                new SimpleMarkedEdge( nodeId, type, IdGenerator.createId( "subType" ), 10005l, false );

        List<MarkedEdge> iteratedEdges = new ArrayList<>( ( int ) shardCount );

        for ( long i = 0; i < shardCount - 1; i++ ) {
            iteratedEdges.add( skippedEdge );
        }

        iteratedEdges.add( returnedEdge );

        //return a shard size equal to our max
        when( nodeShardApproximation.getCount( scope, futureShard, targetEdgeMeta ) ).thenReturn( shardCount );

        ArgumentCaptor<Shard> shardValue = ArgumentCaptor.forClass( Shard.class );


        //mock up our mutation
        when( edgeShardSerialization.writeShardMeta( same( scope ), shardValue.capture(), same( targetEdgeMeta ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final Iterator<MarkedEdge> edgeIterator = iteratedEdges.iterator();

        //mock up returning the value
        when( shardedEdgeSerialization
                .getEdgesFromSourceByTargetType( same( edgeColumnFamilies ), same( scope ), any( SearchByIdType.class ),
                        any( Collection.class ) ) ).thenReturn( edgeIterator );


        final boolean result = approximation.auditShard( scope, shardEntryGroup, targetEdgeMeta );

        assertTrue( "Shard allocated", result );

        //check our new allocated UUID


        final long savedTimestamp = shardValue.getValue().getCreatedTime();


        assertEquals( "Expected time service time", timeservicetime, savedTimestamp );


        //now check our max value was set

        final long savedShardPivot = shardValue.getValue().getShardIndex();

        assertEquals( "Expected max value to be the same", returnedEdge.getTimestamp(), savedShardPivot );
    }


    @Test
    public void invalidCountNoShards() {

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        final long timeservicetime = System.currentTimeMillis();

        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        final Shard futureShard = new Shard( 0l, 0l, true );

        final ShardEntryGroup shardEntryGroup = new ShardEntryGroup( 1000l );
        shardEntryGroup.addShard( futureShard );

        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( nodeId, type, subType );

        final long shardCount = graphFig.getShardSize();

        //return a shard size equal to our max
        when( nodeShardApproximation.getCount( scope, futureShard, targetEdgeMeta ) ).thenReturn( shardCount );

        ArgumentCaptor<Shard> shardValue = ArgumentCaptor.forClass( Shard.class );


        //mock up our mutation
        when( edgeShardSerialization.writeShardMeta( same( scope ), shardValue.capture(), same( targetEdgeMeta ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final SimpleMarkedEdge returnedEdge =
                new SimpleMarkedEdge( nodeId, type, IdGenerator.createId( "subType" ), 10005l, false );

        final Iterator<MarkedEdge> edgeIterator = Collections.singleton( ( MarkedEdge ) returnedEdge ).iterator();

        //mock up returning the value
        when( shardedEdgeSerialization
                .getEdgesFromSourceByTargetType( same( edgeColumnFamilies ), same( scope ), any( SearchByIdType.class ),
                        any( Collection.class ) ) ).thenReturn( edgeIterator );


        final boolean result = approximation.auditShard( scope, shardEntryGroup, targetEdgeMeta );

        assertFalse( "Shard should not be allocated", result );
    }


    @Test
    public void futureCountShardCleanup() {

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );


        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";


        /**
         * Use the time service to generate timestamps
         */
        final long timeservicetime = System.currentTimeMillis() + 60000;


        when( timeService.getCurrentTime() ).thenReturn( timeservicetime );

        assertTrue( "Shard cache mocked", graphFig.getShardCacheTimeout() > 0 );


        /**
         * Simulates clock drift when 2 nodes create future shards near one another
         */
        final long minDelta = graphFig.getShardMinDelta();


        final Shard minShard = new Shard( 0l, 0l, true );

        //a shard that isn't our minimum, but exists after compaction
        final Shard compactedShard = new Shard( 5000, 1000, true );

        /**
         * Simulate different node time allocation
         */

        final long minTime = 10000;
        //our second shard is the "oldest", and hence should be returned in the iterator.  Future shard 1 and 3
        // should be removed

        //this should get dropped, It's allocated after future shard2 even though the time is less
        final Shard futureShard1 = new Shard( 10000, minTime + minDelta, false );

        //should get kept.
        final Shard futureShard2 = new Shard( 10005, minTime, false );

        //should be removed
        final Shard futureShard3 = new Shard( 10010, minTime + minDelta / 2, false );

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( nodeId, type, subType );

        /**
         * Mock up returning a min shard
         */
        when( edgeShardSerialization
                .getShardMetaData( same( scope ), any( Optional.class ), same( directedEdgeMeta ) ) ).thenReturn(
                Arrays.asList( futureShard3, futureShard2, futureShard1, compactedShard, minShard ).iterator() );


        ArgumentCaptor<Shard> newLongValue = ArgumentCaptor.forClass( Shard.class );


        //mock up our mutation
        when( edgeShardSerialization
                .removeShardMeta( same( scope ), newLongValue.capture(), same( directedEdgeMeta ) ) )
                .thenReturn( mock( MutationBatch.class ) );


        final Iterator<ShardEntryGroup> result =
                approximation.getShards( scope, Optional.<Shard>absent(), directedEdgeMeta );


        assertTrue( "Shards present", result.hasNext() );


        ShardEntryGroup shardEntryGroup = result.next();

        assertEquals( "Future shard returned", futureShard1, shardEntryGroup.getCompactionTarget() );


        //now verify all 4 are in this group.  This is because the first shard (0,0) (n-1_ may be the only shard other
        //nodes see while we're rolling our state.  This means it should be read and merged from as well

        Collection<Shard> writeShards = shardEntryGroup.getWriteShards( minTime + minDelta );

        assertEquals( "Shard size as expected", 1, writeShards.size() );

        assertTrue( writeShards.contains( compactedShard ) );


        Collection<Shard> readShards = shardEntryGroup.getReadShards();

        assertEquals( "Shard size as expected", 2, readShards.size() );

        assertTrue( readShards.contains( futureShard1 ) );
        assertTrue( readShards.contains( compactedShard ) );


        assertTrue( "Shards present", result.hasNext() );

        shardEntryGroup = result.next();


        writeShards = shardEntryGroup.getWriteShards( minTime + minDelta );


        assertTrue( "Previous shard present", writeShards.contains( minShard ) );


        writeShards = shardEntryGroup.getReadShards();


        assertTrue( "Previous shard present", writeShards.contains( minShard ) );


        assertFalse( "No shards left", result.hasNext() );
    }


    @Test
    public void noShardsReturns() throws ConnectionException {

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        final TimeService timeService = mock( TimeService.class );

        final long returnTime = System.currentTimeMillis() + graphFig.getShardCacheTimeout() * 2;

        when( timeService.getCurrentTime() ).thenReturn( returnTime );

        final MutationBatch batch = mock( MutationBatch.class );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );

        final Id nodeId = IdGenerator.createId( "test" );
        final String type = "type";
        final String subType = "subType";

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( nodeId, type, subType );


        /**
         * Mock up returning an empty iterator, our audit shouldn't create a new shard
         */
        when( edgeShardSerialization
                .getShardMetaData( same( scope ), any( Optional.class ), same( directedEdgeMeta ) ) )
                .thenReturn( Collections.<Shard>emptyList().iterator() );


        ArgumentCaptor<Shard> shardArgumentCaptor = ArgumentCaptor.forClass( Shard.class );

        when( edgeShardSerialization
                .writeShardMeta( same( scope ), shardArgumentCaptor.capture(), same( directedEdgeMeta ) ) )
                .thenReturn( batch );


        final Iterator<ShardEntryGroup> result =
                approximation.getShards( scope, Optional.<Shard>absent(), directedEdgeMeta );


        ShardEntryGroup shardEntryGroup = result.next();

        final Shard rootShard = new Shard( 0, 0, true );

        assertEquals( "Shard size expected", 1, shardEntryGroup.entrySize() );


        //ensure we persisted the new shard.
        assertEquals( "Root shard was persisted", rootShard, shardArgumentCaptor.getValue() );


        //now verify all 4 are in this group.  This is because the first shard (0,0) (n-1_ may be the only shard other
        //nodes see while we're rolling our state.  This means it should be read and merged from as well

        Collection<Shard> writeShards = shardEntryGroup.getWriteShards( timeService.getCurrentTime() );

        Collection<Shard> readShards = shardEntryGroup.getReadShards();


        assertTrue( "root shard allocated", writeShards.contains( rootShard ) );

        assertTrue( "root shard allocated", readShards.contains( rootShard ) );


        assertFalse( "No other shard group allocated", result.hasNext() );
    }


    @Test
    public void invalidConfiguration() {

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final GraphFig graphFig = mock( GraphFig.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final NodeShardApproximation nodeShardApproximation = mock( NodeShardApproximation.class );


        /**
         * Return 100000 milliseconds
         */
        final TimeService timeService = mock( TimeService.class );

        final long time = 100000l;

        when( timeService.getCurrentTime() ).thenReturn( time );


        final long cacheTimeout = 30000l;

        when( graphFig.getShardCacheTimeout() ).thenReturn( 30000l );


        final long tooSmallDelta = ( long ) ( ( cacheTimeout * 2 ) * .99 );

        when( graphFig.getShardMinDelta() ).thenReturn( tooSmallDelta );

        NodeShardAllocation approximation =
                new NodeShardAllocationImpl( edgeShardSerialization, edgeColumnFamilies, shardedEdgeSerialization,
                        nodeShardApproximation, timeService, graphFig, shardGroupCompaction );


        /**
         * Should throw an exception
         */
        try {
            approximation.getMinTime();
            fail( "Should have thrown a GraphRuntimeException" );
        }
        catch ( GraphRuntimeException gre ) {
            //swallow
        }

        //now test something that passes.

        final long minDelta = cacheTimeout * 2;

        when( graphFig.getShardMinDelta() ).thenReturn( minDelta );

        long returned = approximation.getMinTime();

        long expectedReturned = time - minDelta;

        assertEquals( expectedReturned, returned );

        final long delta = cacheTimeout * 4;

        when( graphFig.getShardMinDelta() ).thenReturn( delta );

        returned = approximation.getMinTime();

        expectedReturned = time - delta;

        assertEquals( expectedReturned, returned );
    }
}
