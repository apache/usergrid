/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.graph;


import java.util.concurrent.TimeoutException;

import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( ITRunner.class )
@UseModules( TestGraphModule.class )
public class GraphManagerShardingIT {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected GraphManagerFactory emf;


    @Inject
    protected GraphFig graphFig;

    @Inject
    protected NodeShardApproximation nodeShardApproximation;

    protected ApplicationScope scope;




    @Before
    public void mockApp() {
        this.scope = new ApplicationScopeImpl(createId("application")  );
    }


    @Test
    public void testWriteSourceType() throws TimeoutException, InterruptedException {

        GraphManager gm = emf.createEdgeManager( scope ) ;

        final Id sourceId = createId( "source" );
        final String edgeType = "test";




        final long flushCount = graphFig.getCounterFlushCount();
        final long maxShardSize = graphFig.getShardSize();




        final long startTime = System.currentTimeMillis();

        //each edge causes 4 counts
        final long writeCount = flushCount/4;

        assertTrue( "Shard size must be >= flush Count", maxShardSize >= flushCount );

        Id targetId = null;

        for(long i = 0; i < writeCount; i ++){
            targetId = createId("target") ;

            final Edge edge = createEdge( sourceId, edgeType, targetId);

            gm.writeEdge( edge ).toBlocking().last();

        }


        long shardCount = nodeShardApproximation.getCount( scope, sourceId, 0l, edgeType );

        assertEquals("Shard count for source node should be the same as write count", writeCount, shardCount);


        //now verify it's correct for the target

        shardCount = nodeShardApproximation.getCount( scope, targetId, 0l, edgeType );

        assertEquals(1, shardCount);

    }


    @Test
    public void testWriteTargetType() throws TimeoutException, InterruptedException {

        GraphManager gm = emf.createEdgeManager( scope ) ;

        final Id targetId = createId( "target" );
        final String edgeType = "test";




        final long flushCount = graphFig.getCounterFlushCount();
        final long maxShardSize = graphFig.getShardSize();




        final long startTime = System.currentTimeMillis();

        //each edge causes 4 counts
        final long writeCount = flushCount/4;

        assertTrue( "Shard size must be >= flush Count", maxShardSize >= flushCount );

        Id sourceId = null;

        for(long i = 0; i < writeCount; i ++){
            sourceId = createId("source") ;

            final Edge edge = createEdge( sourceId, edgeType, targetId);

            gm.writeEdge( edge ).toBlocking().last();

        }


        long shardCount = nodeShardApproximation.getCount( scope, targetId, 0l, edgeType );

        assertEquals("Shard count for source node should be the same as write count", writeCount, shardCount);


        //now verify it's correct for the target

        shardCount = nodeShardApproximation.getCount( scope, sourceId, 0l, edgeType );

        assertEquals(1, shardCount);

    }




}





