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


import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.astyanax.MultiTennantColumnFamilyDefinition;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.WriteAheadLog;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.retry.RetryPolicy;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NodeShardApproximationTest {

    private static final Logger LOG = LoggerFactory.getLogger( NodeShardApproximation.class );

    private GraphFig graphFig;

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

        when( graphFig.getShardSize() ).thenReturn( 250000l );
        when( graphFig.getCounterFlushQueueSize() ).thenReturn( 10000 );

        nodeShardCounterSerialization = mock( NodeShardCounterSerialization.class );

        when( nodeShardCounterSerialization.flush( any( Counter.class ) ) ).thenReturn( mock( MutationBatch.class ) );


        timeService = mock( TimeService.class );

        when( timeService.getCurrentTime() ).thenReturn( System.currentTimeMillis() );
    }


    @Test
    public void testSingleShard() throws InterruptedException {


        when( graphFig.getCounterFlushCount() ).thenReturn( 100000l );
        NodeShardApproximation approximation =
                new NodeShardApproximationImpl( graphFig, nodeShardCounterSerialization, timeService );


        final Id id = IdGenerator.createId( "test" );
        final Shard shard = new Shard( 0, 0, true );
        final String type = "type";
        final String type2 = "subType";

        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType( id, type, type2 );

        long count = approximation.getCount( scope, shard, directedEdgeMeta );

        waitForFlush( approximation );

        assertEquals( 0, count );
    }


    private void waitForFlush( NodeShardApproximation approximation ) throws InterruptedException {

        approximation.beginFlush();

        while ( approximation.flushPending() ) {

            LOG.info( "Waiting on beginFlush to complete" );

            Thread.sleep( 100 );
        }
    }


}
