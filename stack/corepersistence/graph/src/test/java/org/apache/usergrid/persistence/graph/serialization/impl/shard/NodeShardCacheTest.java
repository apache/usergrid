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
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardCacheImpl;
import org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test for the shard that mocks responses from the serialization
 */
public class NodeShardCacheTest {


    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    @Test
    public void testNoShards() throws ConnectionException {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );

        final Id id = createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        UUID newTime = UUIDGenerator.newTimeUUID();


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        final Optional max = Optional.absent();
        /**
         * Simulate returning no shards at all.
         */
        when( allocation
                .getShards( same( scope ), same( id ), same( max), same( edgeType ),
                        same( otherIdType ) ) )
                .thenReturn( Collections.singletonList( 0l ).iterator() );


        long slice = cache.getSlice( scope, id, newTime, edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals(0l, slice );


        /**
         * Verify that we fired the audit
         */
        verify( allocation ).auditMaxShard( scope, id, edgeType, otherIdType );
    }


    @Test
    public void testSingleExistingShard() {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );


        final Id id = createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        UUID newTime = UUIDGenerator.newTimeUUID();

        final long min = 0;


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        final Optional max = Optional.absent();

        /**
         * Simulate returning single shard
         */
        when( allocation.getShards( same( scope ), same( id ), same(max),
                same( edgeType ), same( otherIdType ) ) ).thenReturn( Collections.singletonList( min ).iterator() );


        long slice = cache.getSlice( scope, id, newTime, edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );

        /**
         * Verify that we fired the audit
         */
        verify( allocation ).auditMaxShard( scope, id, edgeType, otherIdType );
    }


    @Test
    public void testRangeShard() {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );

        final Id id = createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        /**
         * Set our min mid and max
         */
        final long min = 0;


        final long mid = 10000;


        final long max = 20000;


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        /**
         * Simulate returning all shards
         */
        when( allocation.getShards( same( scope ), same( id ), any( Optional.class ),
                same( edgeType ), same( otherIdType ) ) ).thenReturn( Arrays.asList( min, mid, max ).iterator() );


        //check getting equal to our min, mid and max

        long slice = cache.getSlice( scope, id, EdgeTestUtils.setTimestamp(min), edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );

        slice = cache.getSlice( scope, id, EdgeTestUtils.setTimestamp(mid),
                edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );

        slice = cache.getSlice( scope, id, EdgeTestUtils.setTimestamp(max) ,
                edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( max, slice );

        //now test in between
        slice = cache.getSlice( scope, id, EdgeTestUtils.setTimestamp( min+1 ), edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );

        slice = cache.getSlice( scope, id,  EdgeTestUtils.setTimestamp(  mid-1 ), edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );


        slice = cache.getSlice( scope, id,  EdgeTestUtils.setTimestamp(  mid+1 ), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );

        slice = cache.getSlice( scope, id,  EdgeTestUtils.setTimestamp( max-1), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );


        slice = cache.getSlice( scope, id,  EdgeTestUtils.setTimestamp(  max ), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( max, slice );

        /**
         * Verify that we fired the audit
         */
        verify( allocation ).auditMaxShard( scope, id, edgeType, otherIdType );
    }


    @Test
    public void testRangeShardIterator() {

        final GraphFig graphFig = getFigMock();

        final NodeShardAllocation allocation = mock( NodeShardAllocation.class );

        final Id id = createId( "test" );

        final String edgeType = "edge";

        final String otherIdType = "type";


        /**
         * Set our min mid and max
         */
        final long min = 1;


        final long mid = 100;


        final long max = 200;


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        /**
         * Simulate returning all shards
         */
        when( allocation.getShards( same( scope ), same( id ),  any(Optional.class),
                same( edgeType ), same( otherIdType ) ) ).thenReturn( Arrays.asList( min, mid, max ).iterator() );


        //check getting equal to our min, mid and max

        Iterator<Long> slice =
                cache.getVersions( scope, id,  EdgeTestUtils.setTimestamp(  max ), edgeType, otherIdType );


        assertEquals( max, slice.next().longValue() );
        assertEquals( mid, slice.next().longValue() );
        assertEquals( min, slice.next().longValue() );


        slice = cache.getVersions( scope, id,  EdgeTestUtils.setTimestamp(  mid ),
                edgeType, otherIdType );

        assertEquals( mid, slice.next().longValue() );
        assertEquals( min, slice.next().longValue() );


        slice = cache.getVersions( scope, id,  EdgeTestUtils.setTimestamp(  min ),
                edgeType, otherIdType );

        assertEquals( min, slice.next().longValue() );


    }


    private GraphFig getFigMock() {
        final GraphFig graphFig = mock( GraphFig.class );
        when( graphFig.getShardCacheSize() ).thenReturn( 1000l );
        when( graphFig.getShardCacheTimeout() ).thenReturn( 30000l );

        return graphFig;
    }
}
