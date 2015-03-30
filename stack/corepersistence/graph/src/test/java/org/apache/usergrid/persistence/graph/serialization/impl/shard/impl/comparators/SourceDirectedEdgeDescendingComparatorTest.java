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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.comparators;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.impl.SimpleMarkedEdge;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class SourceDirectedEdgeDescendingComparatorTest {

    final SourceDirectedEdgeDescendingComparator comp = SourceDirectedEdgeDescendingComparator.INSTANCE;


    @Test
    public void sameEdges() {

        final Id sourceId = IdGenerator.createId( "source" );
        final Id targetId = IdGenerator.createId( "target" );
        final String type = "type";
        final long timestamp = 10000;

        final SimpleMarkedEdge markedEdge1 = new SimpleMarkedEdge( sourceId, type, targetId, timestamp, true );
        final SimpleMarkedEdge markedEdge2 = new SimpleMarkedEdge( sourceId, type, targetId, timestamp, true );


        int compare = comp.compare( markedEdge1, markedEdge2 );

        assertEquals( 0, compare );

        compare = comp.compare( markedEdge2, markedEdge1 );

        assertEquals( 0, compare );
    }


    @Test
    public void timestampDifferent() {

        final Id sourceId = IdGenerator.createId( "source" );
        final Id targetId = IdGenerator.createId( "target" );
        final String type = "type";
        final long timestamp = 10000;

        final SimpleMarkedEdge markedEdge1 = new SimpleMarkedEdge( sourceId, type, targetId, timestamp, true );
        final SimpleMarkedEdge markedEdge2 = new SimpleMarkedEdge( sourceId, type, targetId, timestamp + 1, true );


        //marked edge 1 is less than timestamp, it should be considered "greater"
        int compare = comp.compare( markedEdge1, markedEdge2 );

        assertEquals( 1, compare );

        compare = comp.compare( markedEdge2, markedEdge1 );

        assertEquals( -1, compare );
    }


    @Test
    public void uuidDifferent() {

        final Id sourceId1 = IdGenerator.createId( "source" );
        final Id sourceId2 = IdGenerator.createId( "source" );
        final Id targetId = IdGenerator.createId( "target" );
        final String type = "type";
        final long timestamp = 10000;

        final SimpleMarkedEdge markedEdge1 = new SimpleMarkedEdge( sourceId1, type, targetId, timestamp, true );
        final SimpleMarkedEdge markedEdge2 = new SimpleMarkedEdge( sourceId2, type, targetId, timestamp, true );


        //marked edge 1 uuid is a is less than target uuid timestamp, it should be considered "greater"
        int compare = comp.compare( markedEdge1, markedEdge2 );

        assertTrue( compare > 0 );

        compare = comp.compare( markedEdge2, markedEdge1 );

        assertTrue( compare < 0 );
    }


    @Test
    public void idTypeDifferent() {

        final UUID sourceId = UUIDGenerator.newTimeUUID();

        final Id sourceId1 = IdGenerator.createId( sourceId, "source1" );
        final Id sourceId2 = IdGenerator.createId( sourceId, "source2" );
        final Id targetId = IdGenerator.createId( "target" );
        final String type = "type";
        final long timestamp = 10000;

        final SimpleMarkedEdge markedEdge1 = new SimpleMarkedEdge( sourceId2, type, targetId, timestamp, true );
        final SimpleMarkedEdge markedEdge2 = new SimpleMarkedEdge( sourceId1, type, targetId, timestamp, true );


        //marked edge 1 is less than timestamp, it should be considered "greater"
        int compare = comp.compare( markedEdge1, markedEdge2 );

        assertEquals( 1, compare );

        compare = comp.compare( markedEdge2, markedEdge1 );

        assertEquals( -1, compare );
    }
}
