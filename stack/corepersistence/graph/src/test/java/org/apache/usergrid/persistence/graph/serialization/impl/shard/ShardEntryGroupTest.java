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


import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Test for the group functionality
 */
public class ShardEntryGroupTest {

    @Test
    public void singleEntry() {


        Shard rootShard = new Shard( 0, 0, false );

        ShardEntryGroup shardEntryGroup = new ShardEntryGroup( );

        final boolean result = shardEntryGroup.addShard( rootShard );

        assertTrue( "Shard added", result );

        assertFalse( "Single shard cannot be deleted", shardEntryGroup.canBeDeleted( rootShard ) );

        assertNull( "No merge target found", shardEntryGroup.getCompactionTarget() );

        assertFalse( "Merge cannot be run with a single shard", shardEntryGroup.shouldCompact() );
    }


    @Test
    public void minTimestampSelected() {

        Shard firstShard = new Shard( 1000, 1000, false );

        Shard secondShard = new Shard( 1000, 1001, false );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup(  );

        boolean result = shardEntryGroup.addShard( secondShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );



        assertFalse( "First shard cannot be deleted", shardEntryGroup.canBeDeleted( firstShard ) );

        assertFalse( "Second shard cannot be deleted", shardEntryGroup.canBeDeleted( secondShard ) );

        assertFalse( "Duplicate shard id cannot be deleted", shardEntryGroup.canBeDeleted( secondShard ) );

        assertNull( "Can't compact, no min compacted shard present", shardEntryGroup.getCompactionTarget() );



    }


    @Test
    public void testShardTarget() {

        Shard compactedShard = new Shard( 0, 0, true );

        Shard firstShard = new Shard( 1000, 1000, false );

        Shard secondShard = new Shard( 1000, 1001, false );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup( );

        boolean result = shardEntryGroup.addShard( secondShard );


        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( compactedShard );

        assertTrue( " Shard added", result );


        assertFalse( "First shard cannot be deleted", shardEntryGroup.canBeDeleted( firstShard ) );

        assertFalse( "Second shard cannot be deleted", shardEntryGroup.canBeDeleted( secondShard ) );

        assertFalse( "Duplicate shard id cannot be deleted", shardEntryGroup.canBeDeleted( secondShard ) );

        assertEquals( "Min compaction target found", firstShard, shardEntryGroup.getCompactionTarget() );

        //we should compact these
        assertTrue( "Merge should be run", shardEntryGroup.shouldCompact() );

    }



    @Test
    public void lowerTimestampHigherShard() {

        Shard firstShard = new Shard( 1100, 1001, false );

        //should be compacted away. Even though it has a closer value, it's a higher timestamp, so wasn't written first
        Shard secondShard = new Shard( 1000, 1000, false );

        Shard compactedShard = new Shard( 500, 200, true );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup(  );

        boolean result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( secondShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( compactedShard );

        assertTrue( "Shard added", result );



        assertFalse( "First shard cannot be deleted", shardEntryGroup.canBeDeleted( secondShard  ) );

        assertTrue( "Second shard can be deleted", shardEntryGroup.canBeDeleted( firstShard  ) );

        assertEquals( "Can't compact, no min compacted shard present", secondShard, shardEntryGroup.getCompactionTarget() );



    }



    @Test
    public void multipleShardGroups() {

        Shard firstShard = new Shard( 1000, 10000, false );

        Shard secondShard = new Shard( 999, 9000, false );

        Shard compactedShard1 = new Shard( 900, 8000, true );

        Shard compactedShard2 = new Shard( 800, 7000, true );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup(  );

        boolean result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( secondShard );

        assertTrue( " Shard added", result );

        result = shardEntryGroup.addShard( compactedShard1 );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( compactedShard2 );

        assertFalse( "Shouldn't add since it's compacted", result );

        ShardEntryGroup secondGroup = new ShardEntryGroup(  );

        result = secondGroup.addShard( compactedShard2 );

        assertTrue( "Added successfully", result );
    }


    @Test
    public void boundShardGroup() {
        Shard firstShard = new Shard( 1000, 10000, false );

        Shard secondShard = new Shard( 999, 9000, false );

        Shard compactedShard1 = new Shard( 900, 8000, true );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup( );

        boolean result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( secondShard );

        assertTrue( " Shard added", result );

        result = shardEntryGroup.addShard( compactedShard1 );

        assertTrue( "Shard added", result );


        assertTrue( "Shard can be deleted", shardEntryGroup.canBeDeleted( firstShard ) );

        assertFalse( "Compaction shard shard cannot be deleted", shardEntryGroup.canBeDeleted( secondShard ) );

        assertEquals( "Same shard for merge target", secondShard, shardEntryGroup.getCompactionTarget() );

        //Should return true, we can merge
        assertTrue( "Merge cannot be run within min time",
                shardEntryGroup.shouldCompact() );

    }


    /**
     * Ensures that we read from all shards (even the compacted one)
     */
    @Test
    public void getAllReadShards() {

        Shard firstShard = new Shard( 1000, 10000, false );

        Shard secondShard = new Shard( 999, 9000, false );

        Shard compactedShard1 = new Shard( 900, 8000, true );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup(  );

        boolean result = shardEntryGroup.addShard( firstShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( secondShard );

        assertTrue( " Shard added", result );

        result = shardEntryGroup.addShard( compactedShard1 );

        assertTrue( "Shard added", result );

        Collection<Shard> readShards = shardEntryGroup.getReadShards();

        assertEquals( "Shard size correct", 3, readShards.size() );

        assertTrue( "First shard present", readShards.contains( secondShard ) );

        assertTrue( "Second shard present", readShards.contains( compactedShard1 ) );
    }


    /**
     * Ensures that we read from all shards (even the compacted one)
     */
    @Test
    public void getAllWriteShardsNotPastCompaction() {

        Shard ignoredProposedShard = new Shard( 1000, 10000, false );

        Shard newAllocatedCompactionTarget = new Shard( 999, 9000, false );

        Shard compactedShard = new Shard( 900, 8000, true );


        ShardEntryGroup shardEntryGroup = new ShardEntryGroup( );

        boolean result = shardEntryGroup.addShard( ignoredProposedShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( newAllocatedCompactionTarget );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( compactedShard );

        assertTrue( "Shard added", result );


        Collection<Shard> writeShards = shardEntryGroup.getWriteShards(newAllocatedCompactionTarget.getShardIndex() );

        assertEquals( "Shard size correct", 1, writeShards.size() );

        assertTrue( "Lowest new shard present", writeShards.contains( newAllocatedCompactionTarget ) );


        writeShards = shardEntryGroup.getWriteShards( compactedShard.getShardIndex() );

        assertEquals( "Shard size correct", 1, writeShards.size() );

        assertTrue( "Lowest new shard present", writeShards.contains( compactedShard ) );

    }


    @Test( expected = IllegalArgumentException.class )
    public void failsInsertionOrder() {

        Shard secondShard = new Shard( 20000, 10000, false );

        Shard firstShard = new Shard( 10000, 10000, false );

        Shard rootShard = new Shard( 0, 0, false );

        ShardEntryGroup shardEntryGroup = new ShardEntryGroup( );

        boolean result = shardEntryGroup.addShard( secondShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( rootShard );

        assertTrue( "Shard added", result );

        //this should blow up, we can't add a shard in the middle, it must always be greater than the current max

        shardEntryGroup.addShard( firstShard );
    }


    @Test
    public void shardEntryAddList() {

        Shard highShard = new Shard( 30000, 1000, false );

        Shard midShard = new Shard( 20000, 1000, true );

        Shard lowShard = new Shard( 10000, 1000, false );

        ShardEntryGroup shardEntryGroup = new ShardEntryGroup(  );

        boolean result = shardEntryGroup.addShard( highShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( midShard );

        assertTrue( "Shard added", result );

        result = shardEntryGroup.addShard( lowShard );

        assertFalse( "Shard added", result );
    }
}



