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


import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSeriesSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test for the cache that mocks responses from the serialization
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

        final EdgeSeriesSerialization serialization = mock( EdgeSeriesSerialization.class );

        final Id id = createId("test");

        final String edgeType = "edge";

        final String otherIdType = "type";






        UUID newTime = UUIDGenerator.newTimeUUID();



        NodeShardCache cache = new NodeShardCacheImpl(serialization, graphFig  );


        /**
         * Simulate returning no shards at all.
         */
        when(serialization.getEdgeMetaData( same(scope), same(id), same(edgeType), same(otherIdType)  )).thenReturn(
                Collections.<UUID>emptyList() );


        final MutationBatch batch = mock(MutationBatch.class);

        //mock up returning the mutation batch
        when(serialization.writeEdgeMeta(  same( scope ), same( id ), any( UUID.class ), same(edgeType), same(otherIdType))).thenReturn( batch );

        final UUID min = new UUID(0, 1);

        UUID slice = cache.getSlice( scope, id, newTime, edgeType, otherIdType );


        //we return the min UUID possible, all edges should start by writing to this edge
        assertEquals(min, slice);

        /**
         * Verify that we
         */
        verify( serialization).writeEdgeMeta( scope, id, slice, edgeType, otherIdType);

        /**
         * Verify that execute was invoked
         */
        verify(batch).execute();


    }


    @Test
    public void testExistingShard(){

        fail("TODO");
    }

    private GraphFig getFigMock(){
        final GraphFig graphFig = mock( GraphFig.class);
        when(graphFig.getCacheSize()).thenReturn( 1000 );
        when(graphFig.getCacheTimeout()).thenReturn( 30000l );

        return graphFig;
    }

}
