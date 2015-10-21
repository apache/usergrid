/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.executor.TaskExecutorFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.AsyncTaskExecutor;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupDeletion;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.astyanax.MutationBatch;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ShardGroupDeletionImplTest {


    protected AsyncTaskExecutor asyncTaskExecutor;
    protected ListeningExecutorService listeningExecutorService;
    private ApplicationScopeImpl scope;


    @Before
    public void setup() {


        this.scope = new ApplicationScopeImpl( createId( "application" ) );
    }


    @After
    public void shutDown() {
        listeningExecutorService.shutdownNow();
    }


    @Test
    public void shardCannotBeCompacted() throws ExecutionException, InterruptedException {

        final long createTime = 10000;

        final long currentTime = createTime;

        final Shard shard0 = new Shard( 0, createTime, true );

        final Shard shard1 = new Shard( 1000, createTime, false );

        //set a 1 delta for testing
        final ShardEntryGroup group = new ShardEntryGroup( 1 );

        group.addShard( shard1 );
        group.addShard( shard0 );

        assertTrue( "this should return true for our test to succeed", group.isCompactionPending() );


        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final TimeService timeService = mock( TimeService.class );

        when( timeService.getCurrentTime() ).thenReturn( currentTime );

        initExecutor( 1, 1 );

        final ShardGroupDeletionImpl shardGroupDeletion =
            new ShardGroupDeletionImpl( asyncTaskExecutor, edgeShardSerialization, timeService );

        final DirectedEdgeMeta directedEdgeMeta = getDirectedEdgeMeta();


        final ListenableFuture<ShardGroupDeletion.DeleteResult> future =
            shardGroupDeletion.maybeDeleteShard( this.scope, directedEdgeMeta, group, Collections.emptyIterator() );

        final ShardGroupDeletion.DeleteResult result = future.get();

        assertEquals( "should not delete with pending compaction", ShardGroupDeletion.DeleteResult.COMPACTION_PENDING,
            result );
    }


    @Test
    public void shardTooNew() throws ExecutionException, InterruptedException {

        final long createTime = 10000;

        final long currentTime = createTime;

        final Shard shard0 = new Shard( 0, createTime, true );


        ////set a delta for way in the future
        final ShardEntryGroup group = new ShardEntryGroup( 1 );

        group.addShard( shard0 );

        assertFalse( "this should return false for our test to succeed", group.isCompactionPending() );

        assertTrue( "this should return true for our test to succeed", group.isNew( currentTime ) );


        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final TimeService timeService = mock( TimeService.class );

        when( timeService.getCurrentTime() ).thenReturn( currentTime );


        initExecutor( 1, 1 );

        final ShardGroupDeletionImpl shardGroupDeletion =
            new ShardGroupDeletionImpl( asyncTaskExecutor, edgeShardSerialization, timeService );

        final DirectedEdgeMeta directedEdgeMeta = getDirectedEdgeMeta();


        final ListenableFuture<ShardGroupDeletion.DeleteResult> future =
            shardGroupDeletion.maybeDeleteShard( this.scope, directedEdgeMeta, group, Collections.emptyIterator() );

        final ShardGroupDeletion.DeleteResult result = future.get();

        assertEquals( "should not delete within timeout period", ShardGroupDeletion.DeleteResult.TOO_NEW, result );
    }


    @Test
    public void hasEdges() throws ExecutionException, InterruptedException {

        final long createTime = 10000;

        final long currentTime = createTime * 2;

        final Shard shard0 = new Shard( 0, createTime, true );


        ////set a delta for way in the future
        final ShardEntryGroup group = new ShardEntryGroup( 1 );

        group.addShard( shard0 );

        assertFalse( "this should return false for our test to succeed", group.isCompactionPending() );

        assertFalse( "this should return false for our test to succeed", group.isNew( currentTime ) );


        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final TimeService timeService = mock( TimeService.class );

        when( timeService.getCurrentTime() ).thenReturn( currentTime );

        initExecutor( 1, 1 );

        final ShardGroupDeletionImpl shardGroupDeletion =
            new ShardGroupDeletionImpl( asyncTaskExecutor, edgeShardSerialization, timeService );

        final DirectedEdgeMeta directedEdgeMeta = getDirectedEdgeMeta();


        final Iterator<MarkedEdge> notMarkedIterator = Collections.singleton(
            ( MarkedEdge ) new SimpleMarkedEdge( createId( "source" ), "type", createId( "target" ), 1000, false ) )
                                                                  .iterator();


        final ListenableFuture<ShardGroupDeletion.DeleteResult> future =
            shardGroupDeletion.maybeDeleteShard( this.scope, directedEdgeMeta, group, notMarkedIterator );

        final ShardGroupDeletion.DeleteResult result = future.get();

        assertEquals( "should not delete with edges", ShardGroupDeletion.DeleteResult.CONTAINS_EDGES, result );

        //now check when marked we also retain them

        final Iterator<MarkedEdge> markedEdgeIterator = Collections.singleton(
            ( MarkedEdge ) new SimpleMarkedEdge( createId( "source" ), "type", createId( "target" ), 1000, false ) )
                                                                   .iterator();


        final ListenableFuture<ShardGroupDeletion.DeleteResult> markedFuture =
            shardGroupDeletion.maybeDeleteShard( this.scope, directedEdgeMeta, group, markedEdgeIterator );

        final ShardGroupDeletion.DeleteResult markedResult = future.get();

        assertEquals( "should not delete with edges", ShardGroupDeletion.DeleteResult.CONTAINS_EDGES, markedResult );
    }


    @Test
    public void testDeletion() throws ExecutionException, InterruptedException {

        final long createTime = 10000;

        final long currentTime = createTime * 2;

        final Shard shard0 = new Shard( 0, createTime, true );


        ////set a delta for way in the future
        final ShardEntryGroup group = new ShardEntryGroup( 1 );

        group.addShard( shard0 );

        assertFalse( "this should return false for our test to succeed", group.isCompactionPending() );

        assertFalse( "this should return false for our test to succeed", group.isNew( currentTime ) );



        final DirectedEdgeMeta directedEdgeMeta = getDirectedEdgeMeta();

        //mock up returning a mutation
        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );


        when(edgeShardSerialization.removeShardMeta( same(scope), same(shard0), same(directedEdgeMeta) )).thenReturn( mock(
            MutationBatch.class) );

        final TimeService timeService = mock( TimeService.class );

        when( timeService.getCurrentTime() ).thenReturn( currentTime );

        initExecutor( 1, 1 );

        final ShardGroupDeletionImpl shardGroupDeletion =
            new ShardGroupDeletionImpl( asyncTaskExecutor, edgeShardSerialization, timeService );




        final ListenableFuture<ShardGroupDeletion.DeleteResult> future =
            shardGroupDeletion.maybeDeleteShard( this.scope, directedEdgeMeta, group, Collections.emptyIterator() );

        final ShardGroupDeletion.DeleteResult result = future.get();

        assertEquals( "should  delete", ShardGroupDeletion.DeleteResult.DELETED, result );
    }



    private DirectedEdgeMeta getDirectedEdgeMeta() {

        final Id sourceId = createId( "source" );
        final String edgeType = "test";

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( sourceId, edgeType );

        return directedEdgeMeta;
    }


    private void initExecutor( final int numberThreads, final int queueLength ) {
        listeningExecutorService = MoreExecutors.listeningDecorator( TaskExecutorFactory
            .createTaskExecutor( "GraphTaskExecutor", numberThreads, queueLength,
                TaskExecutorFactory.RejectionAction.ABORT ) );

        asyncTaskExecutor = mock( AsyncTaskExecutor.class );

        when( asyncTaskExecutor.getExecutorService() ).thenReturn( listeningExecutorService );
    }
}
