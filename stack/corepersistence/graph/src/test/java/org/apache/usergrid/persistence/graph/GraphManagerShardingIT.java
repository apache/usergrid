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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith( ITRunner.class )
@UseModules( TestGraphModule.class )
@Ignore("Kills cassandra")
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
        this.scope = new ApplicationScopeImpl( IdGenerator.createId( "application" )  );
    }


    @Test
    public void testWriteSourceType() throws TimeoutException, InterruptedException {

        GraphManager gm = emf.createEdgeManager( scope ) ;

        final Id sourceId = IdGenerator.createId( "source" );
        final String edgeType = "test";




        final long flushCount = graphFig.getCounterFlushCount();
        final long maxShardSize = graphFig.getShardSize();




        final long startTime = System.currentTimeMillis();

        //each edge causes 4 counts
        final long writeCount = flushCount/4;

        assertTrue( "Shard size must be >= beginFlush Count", maxShardSize >= flushCount );

        Id targetId = null;

        for(long i = 0; i < writeCount; i ++){
            targetId = IdGenerator.createId( "target" ) ;

            final Edge edge = createEdge( sourceId, edgeType, targetId);

            gm.writeEdge( edge ).toBlocking().last();

        }



        final DirectedEdgeMeta sourceEdgeMeta = DirectedEdgeMeta.fromSourceNodeTargetType( sourceId, edgeType,
                targetId.getType() );
        final Shard shard = new Shard(0, 0, true);


        long shardCount = nodeShardApproximation.getCount( scope, shard, sourceEdgeMeta );

        assertEquals("Shard count for source node should be the same as write count", writeCount, shardCount);


        //now verify it's correct for the target
        final DirectedEdgeMeta targetEdgeMeta = DirectedEdgeMeta.fromTargetNodeSourceType(targetId,  edgeType, sourceId.getType() );


        shardCount = nodeShardApproximation.getCount( scope, shard, targetEdgeMeta );

        assertEquals(1, shardCount);

    }


    @Test
    public void testWriteTargetType() throws TimeoutException, InterruptedException {

        GraphManager gm = emf.createEdgeManager( scope ) ;

        final Id targetId = IdGenerator.createId( "target" );
        final String edgeType = "test";




        final long flushCount = graphFig.getCounterFlushCount();
        final long maxShardSize = graphFig.getShardSize();


         //each edge causes 4 counts
        final long writeCount = flushCount/4;

        assertTrue( "Shard size must be >= beginFlush Count", maxShardSize >= flushCount );

        Id sourceId = null;

        for(long i = 0; i < writeCount; i ++){
            sourceId = IdGenerator.createId( "source" ) ;

            final Edge edge = createEdge( sourceId, edgeType, targetId);

            gm.writeEdge( edge ).toBlocking().last();

        }


        //this is from target->source, since the target id doesn't change
        final DirectedEdgeMeta targetMeta = DirectedEdgeMeta.fromTargetNode( targetId, edgeType );
        final Shard shard = new Shard(0l, 0l, true);

        long targetWithType = nodeShardApproximation.getCount( scope, shard, targetMeta );

        assertEquals("Shard count for target node should be the same as write count", writeCount, targetWithType);


        final DirectedEdgeMeta targetNodeSource = DirectedEdgeMeta.fromTargetNodeSourceType( targetId, edgeType, "source" );

        long shardCount = nodeShardApproximation.getCount( scope, shard, targetNodeSource );

        assertEquals("Shard count for target node should be the same as write count", writeCount, shardCount);


        //now verify it's correct for the target

        final DirectedEdgeMeta sourceMeta = DirectedEdgeMeta.fromSourceNode( sourceId, edgeType );

        shardCount = nodeShardApproximation.getCount( scope, shard, sourceMeta );

        assertEquals(1, shardCount);

    }




}





