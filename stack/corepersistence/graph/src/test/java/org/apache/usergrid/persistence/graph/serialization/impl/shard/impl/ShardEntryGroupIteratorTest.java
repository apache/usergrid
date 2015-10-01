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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;

import static junit.framework.TestCase.assertTrue;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class ShardEntryGroupIteratorTest {


    @Test(expected = IllegalArgumentException.class)
    public void noShards() {

        final ApplicationScope scope = new ApplicationScopeImpl( IdGenerator.createId( "application" ) );
        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( IdGenerator.createId( "source" ), "test" );
        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );
        final long delta = 10000;
        final Iterator<Shard> noShards = Collections.<Shard>emptyList().iterator();

        //should blow up, our iterator is empty
        new ShardEntryGroupIterator( noShards, delta, shardGroupCompaction, scope, directedEdgeMeta );
    }


    @Test
    public void existingSingleShard() {

        final ApplicationScope scope = new ApplicationScopeImpl( IdGenerator.createId( "application" ) );
        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( IdGenerator.createId( "source" ), "test" );


        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );

        final Shard minShard = new Shard( 0, 0, true );
        final long delta = 10000;
        final Iterator<Shard> noShards = Collections.singleton( minShard ).iterator();

        ShardEntryGroupIterator entryGroupIterator =
                new ShardEntryGroupIterator( noShards, delta, shardGroupCompaction, scope, directedEdgeMeta );


        assertTrue( "Root shard always present", entryGroupIterator.hasNext() );

        ShardEntryGroup group = entryGroupIterator.next();

        assertNotNull( "Group returned", group );

        //verify we ran our compaction check
        verify( shardGroupCompaction ).evaluateShardGroup( same( scope ), same( directedEdgeMeta ), eq( group ) );


        Collection<Shard> readShards = group.getReadShards();

        assertEquals( "Min shard present", 1, readShards.size() );

        assertTrue( "Min shard present", readShards.contains( minShard ) );


        Collection<Shard> writeShards = group.getWriteShards( 0 );

        assertEquals( "Min shard present", 1, writeShards.size() );

        assertTrue( "Min shard present", writeShards.contains( minShard ) );


        writeShards = group.getWriteShards( Long.MAX_VALUE );

        assertEquals( "Min shard present", 1, writeShards.size() );

        assertTrue( "Min shard present", writeShards.contains( minShard ) );
    }


    /**
     * Tests the iterator constructs boundaries between groups correctly.  In a "real" runtime environment, I expect
     * that only the last 1 or 2 groups will actually have more than 1 entry.
     */
    @Test
    public void boundedShardSets() {

        final ApplicationScope scope = new ApplicationScopeImpl( IdGenerator.createId( "application" ) );
        final DirectedEdgeMeta directedEdgeMeta = DirectedEdgeMeta.fromSourceNode( IdGenerator.createId( "source" ), "test" );

        final ShardGroupCompaction shardGroupCompaction = mock( ShardGroupCompaction.class );


        /**
         * Next shard group
         */
        final Shard shardGroup1Shard1 = new Shard( 0, 0, true );

        final Shard shardGroup1Shard2 = new Shard( 10000, 100, false );

        final Shard shardGroup1Shard3 = new Shard( 20000, 200, false );


        /**
         * Middle shard group
         */
        final Shard shardGroup2Shard1 = new Shard( 30000, 300, true );

        final Shard shardGroup2Shard2 = new Shard( 40000, 400, false );


        /**
         * Highest shard group
         */

        final Shard shardGroup3Shard1 = new Shard( 50000, 500, true );

        final Shard shardGroup3Shard2 = new Shard( 60000, 600, false );

        final Shard shardGroup3Shard3 = new Shard( 70000, 700, false );


        final long delta = 10000;

        final Iterator<Shard> noShards =
                Arrays.asList( shardGroup3Shard3, shardGroup3Shard2, shardGroup3Shard1, shardGroup2Shard2,
                        shardGroup2Shard1, shardGroup1Shard3, shardGroup1Shard2, shardGroup1Shard1 ).iterator();


        ShardEntryGroupIterator entryGroupIterator =
                new ShardEntryGroupIterator( noShards, delta, shardGroupCompaction, scope, directedEdgeMeta );

        assertTrue( "max group present", entryGroupIterator.hasNext() );

        ShardEntryGroup group = entryGroupIterator.next();

        assertNotNull( "Group returned", group );

        //verify we ran our compaction check
        verify( shardGroupCompaction ).evaluateShardGroup( same( scope ), same( directedEdgeMeta ), eq( group ) );

        Collection<Shard> readShards = group.getReadShards();

        assertEquals( "Both shards present", 2, readShards.size() );

        assertTrue( "shardGroup3Shard2 shard present", readShards.contains( shardGroup3Shard2 ) );

        assertTrue( "shardGroup3Shard1 shard present", readShards.contains( shardGroup3Shard1 ) );


        Collection<Shard> writeShards = group.getWriteShards( 0 );

        assertEquals( "Min shard present", 1, writeShards.size() );


        assertTrue( "shardGroup3Shard1 shard present", writeShards.contains( shardGroup3Shard1 ) );

        writeShards = group.getWriteShards( shardGroup3Shard3.getCreatedTime() + delta );

        assertEquals( "Min shard present", 1, writeShards.size() );


        assertTrue( "shardGroup3Shard2 shard present", readShards.contains( shardGroup3Shard2 ) );

        assertTrue( "shardGroup3Shard1 shard present", writeShards.contains( shardGroup3Shard1 ) );


        /****
         * Middle group
         */

        assertTrue( "middle group present", entryGroupIterator.hasNext() );

        group = entryGroupIterator.next();

        assertNotNull( "Group returned", group );

        //verify we ran our compaction check
        verify( shardGroupCompaction ).evaluateShardGroup( same( scope ), same( directedEdgeMeta ), eq( group ) );


        readShards = group.getReadShards();


        assertEquals( "Both shards present", 2, readShards.size() );

        assertTrue( "shardGroup2Shard1 shard present", readShards.contains( shardGroup2Shard1 ) );

        assertTrue( "shardGroup2Shard2 shard present", readShards.contains( shardGroup2Shard2 ) );


        writeShards = group.getWriteShards( 0 );

        assertEquals( "Min shard present", 1, writeShards.size() );

        assertTrue( "shardGroup2Shard1 shard present", writeShards.contains( shardGroup2Shard1 ) );


        writeShards = group.getWriteShards( shardGroup2Shard2.getCreatedTime() + delta + 1 );

        assertEquals( "Both shards present", 1, writeShards.size() );

        assertTrue( "shardGroup2Shard2 shard present", writeShards.contains( shardGroup2Shard2 ) );


        /*****
         * Minimum group
         */

        assertTrue( "min group present", entryGroupIterator.hasNext() );

        group = entryGroupIterator.next();

        assertNotNull( "Group returned", group );

        //verify we ran our compaction check
        verify( shardGroupCompaction ).evaluateShardGroup( same( scope ), same( directedEdgeMeta ), eq( group ) );


        readShards = group.getReadShards();

        assertEquals( "Both shards present", 2, readShards.size() );

        assertTrue( "shardGroup1Shard1 shard present", readShards.contains( shardGroup1Shard1 ) );
        assertTrue( "shardGroup1Shard2 shard present", readShards.contains( shardGroup1Shard2 ) );


        writeShards = group.getWriteShards( 0 );

        assertEquals( "Min shard present", 1, writeShards.size() );

        assertTrue( "shardGroup1Shard1 shard present", writeShards.contains( shardGroup1Shard1 ) );


        writeShards = group.getWriteShards( shardGroup1Shard3.getCreatedTime() + delta + 1 );

        assertEquals( "Both shards present", 1, writeShards.size() );

        assertTrue( "shardGroup1Shard2 shard present", writeShards.contains( shardGroup1Shard2 ) );
    }
}
