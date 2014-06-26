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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


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

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.MutationBatch;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NodeShardApproximationTest {


    private GraphFig graphFig;

    private EdgeShardCounterSerialization ser;
    private NodeShardCounterSerialization nodeShardCounterSerialization;
    private TimeService timeService;

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
        when( graphFig.getShardSize() ).thenReturn( 250000l );

        ser = mock( EdgeShardCounterSerialization.class );
        nodeShardCounterSerialization = mock( NodeShardCounterSerialization.class );

        when(nodeShardCounterSerialization.flush( any(Counter.class) )).thenReturn( mock( MutationBatch.class) );


        timeService = mock( TimeService.class );
    }


    @Test
    public void testSingleShard() {


        when(graphFig.getCounterFlushCount()).thenReturn( 100000l );

        NodeShardApproximation approximation =
                new NodeShardApproximationImpl( graphFig, nodeShardCounterSerialization, timeService );


        final Id id = createId( "test" );
        final long shardId = 0l;
        final String type = "type";
        final String type2 = "subType";

        long count = approximation.getCount( scope, id, shardId, type, type2 );

        assertEquals( 0, count );
    }


    @Test
    public void testMultipleShard() throws ExecutionException, InterruptedException {


        when(graphFig.getCounterFlushCount()).thenReturn( 10000l );
        //2 minutes
        when(graphFig.getCounterFlushInterval()).thenReturn( 30000l );

        final NodeShardApproximation approximation =
                new NodeShardApproximationImpl( graphFig, nodeShardCounterSerialization, timeService );


        final int increments = 1000000;
//        final int workers = Runtime.getRuntime().availableProcessors() * 2;
        final int workers =  2;

        final Id id = createId( "test" );
        final String type = "type";
        final String type2 = "subType";
        final long shardId = 10000;


        ExecutorService executor = Executors.newFixedThreadPool( workers );

        List<Future<Long>> futures = new ArrayList<>( workers );

        for ( int i = 0; i < workers; i++ ) {

            final Future<Long> future = executor.submit( new Callable<Long>() {
                @Override
                public Long call() throws Exception {

                    for ( int i = 0; i < increments; i++ ) {
                        approximation.increment( scope, id, shardId, 1, type, type2 );
                    }

                    return 0l;
                }
            } );

            futures.add( future );
        }



        for ( Future<Long> future : futures ) {
           future.get();
        }


        //get our count.  It should be accurate b/c we only have 1 instance

        final long returnedCount = approximation.getCount( scope, id, shardId, type, type2);
        final long expected = workers * increments;


        assertEquals(expected, returnedCount);





    }


}
