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

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardCacheImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test for the shard that mocks responses from the serialization
 */
public class NodeShardCacheTest {


    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    public void testNoShards() throws ConnectionException {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );

        final Id id = IdGenerator.createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        final long newTime = 10000l;


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        final Optional max = Optional.absent();


        final ShardEntryGroup group = new ShardEntryGroup( newTime );
        group.addShard( new Shard( 0, 0, true ) );


        DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( id, edgeType, otherIdType );

        /**
         * Simulate returning no shards at all.
         */
        when( allocation.getShards( same( scope ), same( max ), same( directedEdgeMeta ) ) )

                //use "thenAnswer" so we always return the value, even if  it's invoked more than 1 time.
                .thenAnswer( new Answer<Iterator<ShardEntryGroup>>() {

                    @Override
                    public Iterator<ShardEntryGroup> answer( final InvocationOnMock invocationOnMock )
                            throws Throwable {
                        return Collections.singletonList( group ).iterator();
                    }
                } );


        ShardEntryGroup returnedGroup = cache.getWriteShardGroup( scope, newTime, directedEdgeMeta );

        //ensure it's the same group
        assertSame( group, returnedGroup );


        Iterator<ShardEntryGroup> shards = cache.getReadShardGroup( scope, newTime, directedEdgeMeta );

        assertTrue( shards.hasNext() );


        returnedGroup = shards.next();

        assertSame( "Single shard group expected", group, returnedGroup );

        assertFalse( shards.hasNext() );


        //we return the min UUID possible, all edges should start by writing to this edge

        /**
         * Verify that we fired the audit
         *
         * TODO, us the GUAVA Tick to make this happen
         */
        //        verify(and allocation ).auditMaxShard( scope, id, edgeType, otherIdType );
    }


    @Test
    public void testRangeShard() {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );


        final Id id = IdGenerator.createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        /**
         * Set our min mid and max
         */

        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        final Shard minShard = new Shard( 0, 0, true );
        final Shard midShard = new Shard( 10000, 1000, true );
        final Shard maxShard = new Shard( 20000, 2000, true );


        /**
         * Simulate returning all shards
         */
        final ShardEntryGroup minShardGroup = new ShardEntryGroup( 10000 );
        minShardGroup.addShard( minShard );

        final ShardEntryGroup midShardGroup = new ShardEntryGroup( 10000 );
        midShardGroup.addShard( midShard );


        final ShardEntryGroup maxShardGroup = new ShardEntryGroup( 10000 );
        maxShardGroup.addShard( maxShard );


        DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( id, edgeType, otherIdType );


        /**
         * Simulate returning no shards at all.
         */
        when( allocation.getShards( same( scope ), any( Optional.class ), same( directedEdgeMeta ) ) )

                //use "thenAnswer" so we always return the value, even if  it's invoked more than 1 time.
                .thenAnswer( new Answer<Iterator<ShardEntryGroup>>() {

                    @Override
                    public Iterator<ShardEntryGroup> answer( final InvocationOnMock invocationOnMock )
                            throws Throwable {
                        return Arrays.asList( maxShardGroup, midShardGroup, minShardGroup ).iterator();
                    }
                } );


        //check getting equal to our min, mid and max

        ShardEntryGroup writeShard = cache.getWriteShardGroup( scope, minShard.getShardIndex(), directedEdgeMeta );

        assertSame( minShardGroup, writeShard );


        Iterator<ShardEntryGroup> groups = cache.getReadShardGroup( scope, minShard.getShardIndex(), directedEdgeMeta );

        assertTrue( groups.hasNext() );

        assertSame( "min shard expected", minShardGroup, groups.next() );

        assertFalse( groups.hasNext() );


        //mid
        writeShard = cache.getWriteShardGroup( scope, midShard.getShardIndex(), directedEdgeMeta );

        assertSame( midShardGroup, writeShard );


        groups = cache.getReadShardGroup( scope, midShard.getShardIndex(), directedEdgeMeta );

        assertTrue( groups.hasNext() );

        assertSame( "mid shard expected", midShardGroup, groups.next() );

        assertTrue( groups.hasNext() );

        assertSame( "min shard expected", minShardGroup, groups.next() );

        assertFalse( groups.hasNext() );


        //max

        writeShard = cache.getWriteShardGroup( scope, maxShard.getShardIndex(), directedEdgeMeta );

        assertSame( maxShardGroup, writeShard );


        groups = cache.getReadShardGroup( scope, maxShard.getShardIndex(), directedEdgeMeta );

        assertTrue( groups.hasNext() );

        assertSame( "max shard expected", maxShardGroup, groups.next() );

        assertTrue( groups.hasNext() );

        assertSame( "mid shard expected", midShardGroup, groups.next() );


        assertTrue( groups.hasNext() );

        assertSame( "min shard expected", minShardGroup, groups.next() );

        assertFalse( groups.hasNext() );


        //now test at mid +1 to ensure we get mid + min
        writeShard = cache.getWriteShardGroup( scope, midShard.getShardIndex() + 1, directedEdgeMeta );

        assertSame( midShardGroup, writeShard );


        groups = cache.getReadShardGroup( scope, midShard.getShardIndex() + 1, directedEdgeMeta );

        assertTrue( groups.hasNext() );

        assertSame( "mid shard expected", midShardGroup, groups.next() );

        assertTrue( groups.hasNext() );

        assertSame( "min shard expected", minShardGroup, groups.next() );

        assertFalse( groups.hasNext() );


        //now test at mid -1 to ensure we get min
        writeShard = cache.getWriteShardGroup( scope, midShard.getShardIndex() - 1, directedEdgeMeta );

        assertSame( minShardGroup, writeShard );


        groups = cache.getReadShardGroup( scope, midShard.getShardIndex() - 1, directedEdgeMeta );


        assertTrue( groups.hasNext() );

        assertSame( "min shard expected", minShardGroup, groups.next() );

        assertFalse( groups.hasNext() );
    }


    private GraphFig getFigMock() {
        final GraphFig graphFig = mock( GraphFig.class );
        when( graphFig.getShardCacheSize() ).thenReturn( 1000l );
        when( graphFig.getShardCacheTimeout() ).thenReturn( 30000l );

        return graphFig;
    }
}
