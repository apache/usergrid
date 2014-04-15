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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardApproximationImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NodeShardApproximationTest {


    private GraphFig graphFig;


    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );

        graphFig = mock( GraphFig.class );

        when( graphFig.getShardCacheSize() ).thenReturn( 10000l );
        when(graphFig.getShardSize()).thenReturn( 250000l );
    }


    @Test
    public void testSingleShard() {

        EdgeSeriesCounterSerialization ser = mock( EdgeSeriesCounterSerialization.class );

        NodeShardApproximation approximation = new NodeShardApproximationImpl( graphFig );


        final Id id = createId( "test" );
        final long shardId = 0l;
        final String type = "type";
        final String type2 = "subType";

        long count = approximation.getCount( scope, id, shardId, type, type2 );

        assertEquals( 0, count );
    }


    @Test
    public void testMultipleShard() throws ExecutionException, InterruptedException {


        final NodeShardApproximation approximation = new NodeShardApproximationImpl( graphFig );


        final int increments = 1000000;
        final int workers = 100;

        final Id id = createId( "test" );
        final String type = "type";
        final String type2 = "subType";
        final AtomicLong shardIdGenerator = new AtomicLong( );

        ExecutorService executor = Executors.newFixedThreadPool( workers );

        List<Future<Long>> futures = new ArrayList<>( workers );

        for ( int i = 0; i < workers; i++ ) {

            final Future<Long> future = executor.submit( new Callable<Long>() {
                @Override
                public Long call() throws Exception {

                    final long shardId = shardIdGenerator.incrementAndGet();


                    long count = approximation.getCount( scope, id, shardId, type, type2 );

                    assertEquals( 0, count );

                    for ( int i = 0; i < increments; i++ ) {
                        approximation.increment( scope, id, shardId, 1, type, type2 );
                    }

                    return approximation.getCount( scope, id, shardId, type, type2 );
                }
            } );

            futures.add( future );
        }


        for ( Future<Long> future : futures ) {
            final long value = future.get().longValue();

            assertEquals( increments, value );
        }
    }
}
