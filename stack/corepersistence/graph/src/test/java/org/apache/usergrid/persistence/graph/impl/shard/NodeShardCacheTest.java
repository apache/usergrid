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


import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.impl.Constants;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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


        /**
         * Simulate returning no shards at all.
         */
        when( allocation.getShards( same( scope ), same( id ), same(Constants.MAX_UUID), any( Integer.class ), same( edgeType ), same( otherIdType ) ) )
                .thenReturn( Collections.singletonList( Constants.MIN_UUID ).iterator() );


        UUID slice = cache.getSlice( scope, id, newTime, edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( Constants.MIN_UUID, slice );


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

        final UUID min = new UUID( 0, 1 );


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        /**
         * Simulate returning single shard
         */
        when( allocation.getShards( same( scope ), same( id ), same(Constants.MAX_UUID), any( Integer.class ),  same( edgeType ), same( otherIdType ) ) )
                .thenReturn( Collections.singletonList( min ).iterator() );


        UUID slice = cache.getSlice( scope, id, newTime, edgeType, otherIdType );


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
        final UUID min = new UUID( 0, 1 );


        final UUID mid = new UUID( 0, 100 );


        final UUID max = new UUID( 0, 200 );


        NodeShardCache cache = new NodeShardCacheImpl( allocation, graphFig );


        /**
         * Simulate returning all shards
         */
        when( allocation.getShards( same( scope ), same( id ),  same(Constants.MAX_UUID), any( Integer.class ), same( edgeType ), same( otherIdType ) ) )
                .thenReturn( Arrays.asList( min, mid, max ).iterator() );


        //check getting equal to our min, mid and max

        UUID slice = cache.getSlice( scope, id, new UUID( min.getMostSignificantBits(), min.getLeastSignificantBits() ),
                edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );

        slice = cache.getSlice( scope, id, new UUID( mid.getMostSignificantBits(), mid.getLeastSignificantBits() ),
                edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );

        slice = cache.getSlice( scope, id, new UUID( max.getMostSignificantBits(), max.getLeastSignificantBits() ),
                edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( max, slice );

        //now test in between
        slice = cache.getSlice( scope, id, new UUID( 0, 1 ), edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );

        slice = cache.getSlice( scope, id, new UUID( 0, 99 ), edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals( min, slice );


        slice = cache.getSlice( scope, id, new UUID( 0, 101 ), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );

        slice = cache.getSlice( scope, id, new UUID( 0, 199 ), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( mid, slice );


        slice = cache.getSlice( scope, id, new UUID( 0, 201 ), edgeType, otherIdType );


        //we return the mid UUID possible, all edges should start by writing to this edge
        assertEquals( max, slice );

        /**
         * Verify that we fired the audit
         */
        verify( allocation ).auditMaxShard( scope, id, edgeType, otherIdType );
    }


    private GraphFig getFigMock() {
        final GraphFig graphFig = mock( GraphFig.class );
        when( graphFig.getShardCacheSize() ).thenReturn( 1000 );
        when( graphFig.getCacheTimeout() ).thenReturn( 30000l );

        return graphFig;
    }
}
