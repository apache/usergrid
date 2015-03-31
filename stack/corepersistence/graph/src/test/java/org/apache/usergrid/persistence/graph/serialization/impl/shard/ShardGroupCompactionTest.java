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

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Collection;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.ShardGroupCompactionImpl;

import com.netflix.astyanax.Keyspace;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ShardGroupCompactionTest {

    protected GraphFig graphFig;
    protected ApplicationScope scope;


    @Before
    public void setup() {
        graphFig = mock( GraphFig.class );

        when( graphFig.getShardAuditWorkerCount() ).thenReturn( 10 );

        when( graphFig.getShardAuditWorkerQueueSize() ).thenReturn( 1000 );

        this.scope = new ApplicationScopeImpl( IdGenerator.createId( "application" ) );
    }


    @Test
    public void shouldNotCompact() {

        final TimeService timeService = mock( TimeService.class );

        final NodeShardAllocation nodeShardAllocation = mock( NodeShardAllocation.class );

        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );

        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );

        final Keyspace keyspace = mock( Keyspace.class );

        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );

        final TaskExecutor taskExecutor = mock( TaskExecutor.class );

        final long delta = 10000;

        final long createTime = 20000;

        //we shouldn't be able to compact, should throw an exception
        final long timeNow = createTime + delta - 1;

        ShardEntryGroup group = new ShardEntryGroup( delta );
        group.addShard( new Shard( 2000, createTime, false ) );
        group.addShard( new Shard( 1000, 5000, true ) );


        when( timeService.getCurrentTime() ).thenReturn( timeNow );

        ShardGroupCompactionImpl compaction =
                new ShardGroupCompactionImpl( timeService, graphFig, nodeShardAllocation, shardedEdgeSerialization,
                        edgeColumnFamilies, keyspace, edgeShardSerialization, taskExecutor );


        DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( IdGenerator.createId( "source" ), "test" );

        try {
            compaction.compact( this.scope, directedEdgeMeta, group );
            fail( "I should not reach this point" );
        }
        catch ( Throwable t ) {
            assertEquals( "Correct error message returned", "Compaction cannot be run yet.  Ignoring compaction.",
                    t.getMessage() );
        }
    }


    //    /**
    //     * Tests that when we copy edges, we do not actually run the compaction,
    // we can only run it after we get nothing
    //     * and the timeout has elapsed
    //     */
    //    @Test
    //    public void shouldOnlyCopy() {
    //
    //        final TimeService timeService = mock( TimeService.class );
    //
    //        final NodeShardAllocation nodeShardAllocation = mock( NodeShardAllocation.class );
    //
    //        final ShardedEdgeSerialization shardedEdgeSerialization = mock( ShardedEdgeSerialization.class );
    //
    //        final EdgeColumnFamilies edgeColumnFamilies = mock( EdgeColumnFamilies.class );
    //
    //        final Keyspace keyspace = mock( Keyspace.class );
    //
    //        final EdgeShardSerialization edgeShardSerialization = mock( EdgeShardSerialization.class );
    //
    //        final long delta = 10000;
    //
    //        final long createTime = 20000;
    //
    //        //we shouldn't be able to compact, should throw an exception
    //        final long timeNow = createTime + delta ;
    //
    //
    //        final Shard targetShard = new Shard( 2000, createTime, false ) ;
    //        final Shard sourceShard =  new Shard( 1000, 5000, true );
    //        ShardEntryGroup group = new ShardEntryGroup( delta );
    //        group.addShard( targetShard );
    //        group.addShard( sourceShard );
    //
    //
    //        when( timeService.getCurrentTime() ).thenReturn( timeNow );
    //
    //        ShardGroupCompaction compaction =
    //                new ShardGroupCompactionImpl( timeService, graphFig, nodeShardAllocation,
    // shardedEdgeSerialization,
    //                        edgeColumnFamilies, keyspace, edgeShardSerialization );
    //
    //
    //        DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( createId("source"), "test" );
    //
    //
    //        /**
    //         * Mock up returning edges from the source
    //         */
    //
    //        int count = 100;
    //
    //        for(int i = 0; i < count; i ++){
    //
    //
    //
    //            when(shardedEdgeSerialization.getEdgesFromSource( same(edgeColumnFamilies), same(scope), any(
    //                    SearchByEdgeType.class), Matchers.argThat(new ShardSetMatcher( Collections.singleton(
    // sourceShard ) ))/*any(Set.class)*/ ));
    //            edgeMeta.loadEdges( shardedEdgeSerialization, edgeColumnFamilies, scope,
    //
    //                                Collections.singleton( sourceShard ),  SearchByEdgeType.Order.DESCENDING,
    // Long.MAX_VALUE );
    //        }
    //
    //        try {
    //            compaction.compact( this.scope, directedEdgeMeta, group );
    //            fail( "I should not reach this point" );
    //        }catch(Throwable t){
    //            assertEquals("Correct error message returned", "Compaction cannot be run yet.  Ignoring compaction
    // .", t.getMessage());
    //        }
    //
    //    }


    private final class ShardSetMatcher extends BaseMatcher<Collection<Shard>> {

        private final Collection<Shard> expected;


        private ShardSetMatcher( final Collection<Shard> expected ) {this.expected = expected;}


        @Override
        public boolean matches( final Object o ) {
            if ( !( o instanceof Collection ) ) {
                return false;
            }


            Collection<Shard> passedShards = ( Collection<Shard> ) o;

            return passedShards.containsAll( expected );
        }


        @Override
        public void describeTo( final Description description ) {

            StringBuilder builder = new StringBuilder();

            builder.append( "Collection of shards with shards {" );

            for ( Shard shard : expected ) {
                builder.append( shard ).append( "," );
            }

            builder.setLength( builder.length() - 1 );

            description.appendText( builder.toString() );
        }
    }
}
