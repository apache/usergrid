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
package org.apache.usergrid.persistence.graph.serialization.impl;


import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.SearchByIdType;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests merged edges
 */
public class MergedEdgeReaderTest {

    protected ApplicationScope scope;
    protected GraphFig config;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );


        config = mock( GraphFig.class );
        when( config.getScanPageSize() ).thenReturn( 1000 );
    }


    @Test
    public void testOrderedMergeSource() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id sourceId = createId( "source" );
        final String type = "test";


        MarkedEdge commitLogEdge1 = createEdge( sourceId, type, createId( "target1" ) );
        MarkedEdge storageEdge1 = createEdge( sourceId, type, createId( "target2" ) );
        MarkedEdge commitLogEdge2 = createEdge( sourceId, type, createId( "target3" ) );
        MarkedEdge storageEdge2 = createEdge( sourceId, type, createId( "target4" ) );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( storageEdge1.getTimestamp(), commitLogEdge2.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) < 0 );

        SearchByEdgeType searchByEdgeType = createSearchByEdge( sourceId, type, UUIDGenerator.newTimeUUID(), null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesFromSource( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesFromSource( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesFromSource( scope, searchByEdgeType ).toBlockingObservable().getIterator();


        assertEquals( storageEdge2, marked.next() );
        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( storageEdge1, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );


        assertFalse( marked.hasNext() );
    }


    @Test
    public void testOrderedMergeSourceDeleted() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id sourceId = createId( "source" );
        final String type = "test";


        MarkedEdge commitLogEdge1 = createEdge( sourceId, type, createId( "target" ), UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge1 =   createEdge( sourceId, type, commitLogEdge1.getTargetNode(), commitLogEdge1.getTimestamp(), false );
        MarkedEdge commitLogEdge2 =  createEdge( sourceId, type, createId( "target" ), UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge2 =  createEdge( sourceId, type, commitLogEdge2.getTargetNode(), commitLogEdge2.getTimestamp(), false );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) ==  0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) == 0 );

        SearchByEdgeType searchByEdgeType = createSearchByEdge( sourceId, type, UUIDGenerator.newTimeUUID(), null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesFromSource( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesFromSource( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesFromSource( scope, searchByEdgeType ).toBlockingObservable().getIterator();

        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );
        assertFalse( marked.hasNext() );
    }


    @Test
    public void testOrderedMergeSourceTargetType() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id sourceId = createId( "source" );
        final String type = "test";
        final String targetIdType = "target";

        MarkedEdge commitLogEdge1 = createEdge( sourceId, type, createId( targetIdType ) );
        MarkedEdge storageEdge1 = createEdge( sourceId, type, createId( targetIdType ) );
        MarkedEdge commitLogEdge2 = createEdge( sourceId, type, createId( targetIdType ) );
        MarkedEdge storageEdge2 = createEdge( sourceId, type, createId( targetIdType ) );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( storageEdge1.getTimestamp(), commitLogEdge2.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) < 0 );

        SearchByIdType searchByEdgeType =
                createSearchByEdgeAndId( sourceId, type, UUIDGenerator.newTimeUUID(), targetIdType, null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesFromSourceByTargetType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesFromSourceByTargetType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesFromSourceByTargetType( scope, searchByEdgeType ).toBlockingObservable().getIterator();


        assertEquals( storageEdge2, marked.next() );
        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( storageEdge1, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );
        assertFalse( marked.hasNext() );
    }

    @Test
    public void testOrderedMergeSourceTargetTypeDeleted() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id sourceId = createId( "source" );
        final String type = "test";
        final String targetIdType = "target";

        MarkedEdge commitLogEdge1 = createEdge( sourceId, type, createId( targetIdType ), UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge1 = createEdge( sourceId, type, commitLogEdge1.getTargetNode(), commitLogEdge1.getTimestamp(), false );
        MarkedEdge commitLogEdge2 = createEdge( sourceId, type, createId( targetIdType ) , UUIDGenerator.newTimeUUID(), true);
        MarkedEdge storageEdge2 = createEdge( sourceId, type, commitLogEdge2.getTargetNode(), commitLogEdge2.getTimestamp(), false );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) == 0  );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) == 0 );

        SearchByIdType searchByEdgeType =
                createSearchByEdgeAndId( sourceId, type, UUIDGenerator.newTimeUUID(), targetIdType, null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesFromSourceByTargetType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesFromSourceByTargetType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesFromSourceByTargetType( scope, searchByEdgeType ).toBlockingObservable().getIterator();

        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );
        assertFalse( marked.hasNext() );
    }





    @Test
    public void testOrderedMergeTarget() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id targetId = createId( "target" );
        final String type = "test";


        MarkedEdge commitLogEdge1 = createEdge( createId( "source" ), type, targetId );
        MarkedEdge storageEdge1 = createEdge( createId( "source" ), type, targetId );
        MarkedEdge commitLogEdge2 = createEdge( createId( "source" ), type, targetId );
        MarkedEdge storageEdge2 = createEdge( createId( "source" ), type, targetId );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( storageEdge1.getTimestamp(), commitLogEdge2.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) < 0 );

        SearchByEdgeType searchByEdgeType = createSearchByEdge( targetId, type, UUIDGenerator.newTimeUUID(), null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesToTarget( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesToTarget( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesToTarget( scope, searchByEdgeType ).toBlockingObservable().getIterator();


        assertEquals( storageEdge2, marked.next() );
        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( storageEdge1, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );


        assertFalse( marked.hasNext() );
    }


    @Test
    public void testOrderedMergeTargetDeleted() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id targetId = createId( "target" );
        final String type = "test";


        MarkedEdge commitLogEdge1 =
                createEdge( createId( "source" ), type, targetId, UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge1 = createEdge( commitLogEdge1.getSourceNode(), type, commitLogEdge1.getTargetNode(), commitLogEdge1.getTimestamp(), false );
        MarkedEdge commitLogEdge2 =
                createEdge( createId( "source" ), type, targetId, UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge2 = createEdge( commitLogEdge2.getSourceNode(), type, commitLogEdge2.getTargetNode(), commitLogEdge2.getTimestamp(), false );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) ==  0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) == 0 );

        SearchByEdgeType searchByEdgeType = createSearchByEdge( targetId, type, UUIDGenerator.newTimeUUID(), null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesToTarget( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesToTarget( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesToTarget( scope, searchByEdgeType ).toBlockingObservable().getIterator();

        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );

        assertFalse( marked.hasNext() );
    }


    @Test
    public void testOrderedMergeTargetSourceType() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id targetId = createId( "target" );
        final String type = "test";
        final String sourceIdType = "source";

        MarkedEdge commitLogEdge1 = createEdge( createId( sourceIdType), type, targetId );
        MarkedEdge storageEdge1 = createEdge( createId( sourceIdType ), type, targetId );
        MarkedEdge commitLogEdge2 = createEdge( createId( sourceIdType), type, targetId );
        MarkedEdge storageEdge2 = createEdge( createId( sourceIdType ), type, targetId );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( storageEdge1.getTimestamp(), commitLogEdge2.getTimestamp() ) < 0 );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) < 0 );

        SearchByIdType searchByEdgeType =
                createSearchByEdgeAndId( targetId, type, UUIDGenerator.newTimeUUID(), sourceIdType, null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesToTargetBySourceType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesToTargetBySourceType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesToTargetBySourceType( scope, searchByEdgeType ).toBlockingObservable().getIterator();




        assertEquals( storageEdge2, marked.next() );
        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( storageEdge1, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );

        assertFalse( marked.hasNext() );
    }

    @Test
    public void testOrderedMergeTargetSourceTypeDeleted() {

        EdgeSerialization commitLog = mock( EdgeSerialization.class );

        EdgeSerialization storage = mock( EdgeSerialization.class );

        final Id targetId = createId( "target" );
        final String type = "test";
        final String sourceIdType = "target";

        MarkedEdge commitLogEdge1 = createEdge( createId( sourceIdType), type, targetId, UUIDGenerator.newTimeUUID(), true );
        MarkedEdge storageEdge1 = createEdge( commitLogEdge1.getSourceNode(), type, commitLogEdge1.getTargetNode(), commitLogEdge1.getTimestamp(), false );
        MarkedEdge commitLogEdge2 = createEdge( createId( sourceIdType), type, targetId , UUIDGenerator.newTimeUUID(), true);
        MarkedEdge storageEdge2 = createEdge( commitLogEdge2.getSourceNode(), type, commitLogEdge2.getTargetNode(), commitLogEdge2.getTimestamp(), false );

        //verify our versions are as expected
        assertTrue( UUIDComparator.staticCompare( commitLogEdge1.getTimestamp(), storageEdge1.getTimestamp() ) == 0  );
        assertTrue( UUIDComparator.staticCompare( commitLogEdge2.getTimestamp(), storageEdge2.getTimestamp() ) == 0 );

        SearchByIdType searchByEdgeType =
                createSearchByEdgeAndId( targetId, type, UUIDGenerator.newTimeUUID(), sourceIdType, null );

        /**
         * Mock up the commit log
         */
        when( commitLog.getEdgesToTargetBySourceType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( commitLogEdge2, commitLogEdge1 ).iterator() );

        /**
         * Mock up the storage
         */
        when( storage.getEdgesToTargetBySourceType( scope, searchByEdgeType ) )
                .thenReturn( Arrays.asList( storageEdge2, storageEdge1 ).iterator() );

        //now merge the two
        MergedEdgeReader read = new MergedEdgeReaderImpl( commitLog, storage, config );

        Iterator<MarkedEdge> marked =
                read.getEdgesToTargetBySourceType( scope, searchByEdgeType ).toBlockingObservable().getIterator();

        assertEquals( commitLogEdge2, marked.next() );
        assertEquals( commitLogEdge1, marked.next() );

        assertFalse( marked.hasNext() );
    }

}
