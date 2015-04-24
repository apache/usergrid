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
package org.apache.usergrid.persistence.graph.serialization;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.serializers.StringSerializer;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createMarkedEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */

public abstract class EdgeSerializationTest {

    private static final Logger log = LoggerFactory.getLogger( EdgeSerializationTest.class );

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    protected EdgeSerialization serialization;

    @Inject
    protected GraphFig graphFig;

    @Inject
    protected Keyspace keyspace;

    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );

        serialization = getSerialization();
    }


    /**
     * Get the edge Serialization to use
     */
    protected abstract EdgeSerialization getSerialization();


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void mixedEdgeTypes() throws ConnectionException {
        final MarkedEdge edge1 = createEdge( "source", "edge1", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge2", targetId );

        UUID timestamp = UUIDGenerator.newTimeUUID();

        serialization.writeEdge( scope, edge1, timestamp ).execute();
        serialization.writeEdge( scope, edge2, timestamp ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name

        Iterator<MarkedEdge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge1", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge2", now, null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge1", now, null ) );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge2", now, null ) );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    public void testPaging() throws ConnectionException {

        final MarkedEdge edge1 = createEdge( "source", "edge", "target", 0 );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge", targetId, 1 );


        serialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name


        Iterator<MarkedEdge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, edge2 ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, edge1 ) );

        assertFalse( "No results should be returned", results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, edge2 ) );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, edge1 ) );
        assertFalse( "No results should be returned", results.hasNext() );
        //test resume by name
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void directEdgeGets() throws ConnectionException {

        long timestamp = 1000;
        final MarkedEdge edgev1 = createEdge( "source", "edge1", "target", timestamp );

        final Id sourceId = edgev1.getSourceNode();
        final Id targetId = edgev1.getTargetNode();


        final MarkedEdge edgev2 = createEdge( sourceId, "edge1", targetId, timestamp + 1 );

        //we shouldn't get this one back
        final MarkedEdge diffTarget = createEdge( sourceId, "edge1", IdGenerator.createId( "newTarget" ) );

        assertTrue( "Edge version 1 has lower time uuid",
                Long.compare( edgev1.getTimestamp(), edgev2.getTimestamp() ) < 0 );

        //create edge type 2 to ensure we don't get it in results
        final MarkedEdge edgeType2V1 = createEdge( sourceId, "edge2", targetId );


        serialization.writeEdge( scope, edgev1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edgev2, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edgeType2V1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, diffTarget, UUIDGenerator.newTimeUUID() ).execute();

        final long now = System.currentTimeMillis();


        SearchByEdge search = createGetByEdge( sourceId, "edge1", targetId, now, null );

        Iterator<MarkedEdge> results = serialization.getEdgeVersions( scope, search );

        assertEquals( edgev2, results.next() );
        assertEquals( edgev1, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );

        //max version test

        //test max version
        search = createGetByEdge( sourceId, "edge1", targetId, edgev1.getTimestamp(), null );

        results = serialization.getEdgeVersions( scope, search );

        assertEquals( edgev1, results.next() );
        assertFalse( "Max version was honored", results.hasNext() );
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void mixedIdTypes() throws ConnectionException {
        final MarkedEdge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target2" ) );

        final Id targetId2 = edge2.getTargetNode();

        final UUID timestamp = UUIDGenerator.newTimeUUID();

        serialization.writeEdge( scope, edge1, timestamp ).execute();
        serialization.writeEdge( scope, edge2, timestamp ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name

        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId2.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    public void idTypesPaging() throws ConnectionException {
        final long timestamp = 1000;
        final MarkedEdge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target" ), timestamp + 1 );

        final Id targetId2 = edge2.getTargetNode();


        serialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name

        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertEquals( edge1, results.next() );

        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), edge2 ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), edge1 ) );

        assertFalse( results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), edge2 ) );
        assertTrue( results.hasNext() );
        assertEquals(edge1, results.next());
        assertFalse(results.hasNext());


        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), edge1 ) );
        assertFalse( results.hasNext() );
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    public void delete() throws ConnectionException {
        //we purposefully use the same timestamp
        final long timestamp = 1000l;
        final MarkedEdge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target" ), timestamp );

        final Id targetId2 = edge2.getTargetNode();

        serialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name
        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge2, results.next() );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId1, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId2, "edge", now, null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //now we've validated everything exists, lets blitz the data and ensure it's removed

        final UUID timestamp2 = UUIDGenerator.newTimeUUID();

        serialization.deleteEdge( scope, edge1, timestamp2 ).execute();
        serialization.deleteEdge( scope, edge2, timestamp2 ).execute();


        //now we should get nothing for the same queries
        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );

        assertFalse( results.hasNext() );

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId1, "edge", now, null ) );

        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId2, "edge", now, null ) );

        assertFalse( results.hasNext() );
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    public void mark() throws ConnectionException {
        final long timestamp = 1000l;
        final MarkedEdge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final MarkedEdge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target" ), timestamp + 1 );

        final Id targetId2 = edge2.getTargetNode();


        serialization.writeEdge( scope, edge1, UUIDGenerator.newTimeUUID() ).execute();
        serialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name
        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge2, results.next() );
        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId1, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId2, "edge", now, null ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //now we've validated everything exists, lets blitz the data and ensure it's removed

        final MarkedEdge mark1 =
                createEdge( edge1.getSourceNode(), edge1.getType(), edge1.getTargetNode(), edge1.getTimestamp(), true );

        final MarkedEdge mark2 =
                createEdge( edge2.getSourceNode(), edge2.getType(), edge2.getTargetNode(), edge2.getTimestamp(), true );


        final UUID timestamp2 = UUIDGenerator.newTimeUUID();

        serialization.writeEdge( scope, mark1, timestamp2 ).execute();
        serialization.writeEdge( scope, mark2, timestamp2 ).execute();


        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );


        MarkedEdge edge = results.next();

        assertEquals( mark2, edge );
        assertTrue( edge.isDeleted() );


        edge = results.next();

        assertEquals( mark1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        edge = results.next();

        assertEquals( mark2, edge );
        assertTrue( edge.isDeleted() );

        edge = results.next();

        assertEquals( mark1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );

        edge = results.next();

        assertEquals( mark1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );

        edge = results.next();

        assertEquals( mark2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId1, "edge", now, null ) );

        edge = results.next();

        assertEquals( mark1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId2, "edge", now, null ) );

        edge = results.next();

        assertEquals( mark2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        //now we've validated everything exists
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    @Ignore("Kills embedded cassandra")
    public void pageIteration() throws ConnectionException {

        int size = graphFig.getScanPageSize() * 2;

        final Id sourceId = IdGenerator.createId( "source" );
        final String type = "edge";

        Set<Edge> edges = new HashSet<Edge>( size );


        long timestamp = 0;

        for ( int i = 0; i < size; i++ ) {
            final MarkedEdge edge = createEdge( sourceId, type, IdGenerator.createId( "target" ), timestamp );

            serialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ).execute();
            edges.add( edge );

            timestamp++;
        }


        //get our edges out by name
        Iterator<MarkedEdge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, type, timestamp, null ) );

        for ( MarkedEdge edge : new IterableWrapper<>( results ) ) {
            assertTrue( "Removed edge from write set", edges.remove( edge ) );
        }

        assertEquals( "All edges were returned", 0, edges.size() );
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    @Ignore("Kills embedded cassandra")
    public void testIteratorPaging() throws ConnectionException {


        final Id sourceId = IdGenerator.createId( "source" );
        final String edgeType = "edge";
        final Id targetId = IdGenerator.createId( "target" );


        int writeCount = graphFig.getScanPageSize() * 3;


        final MutationBatch batch = keyspace.prepareMutationBatch();

        long timestamp = 10000l;

        for ( int i = 0; i < writeCount; i++ ) {

            final MarkedEdge edge = createEdge( sourceId, edgeType, targetId, timestamp );

            batch.mergeShallow( serialization.writeEdge( scope, edge, UUIDGenerator.newTimeUUID() ) );

            //increment timestamp (not done inline on purpose) If we do System.currentMillis we get the same edge on
            // fast systems
            timestamp++;
        }

        log.info( "Flushing edges" );
        batch.execute();


        Iterator<MarkedEdge> results = serialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, timestamp, null ) );

        verify( results, writeCount );


        //get them all from source
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, timestamp, null ) );

        verify( results, writeCount );


        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, timestamp, targetId.getType(), null ) );

        verify( results, writeCount );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, timestamp, null ) );

        verify( results, writeCount );


        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, timestamp, sourceId.getType(), null ) );

        verify( results, writeCount );
    }


    /**
     * Tests writing 2 edges quickly in succession, then returning them. Was failing for commitlog impl
     */
    @Test
    public void successiveWriteReturnSource() throws ConnectionException {
        final MarkedEdge edge1 = createMarkedEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final UUID timestamp1 = UUIDGenerator.newTimeUUID();
        final UUID timestamp2 = UUIDGenerator.newTimeUUID();
        final UUID timestamp3 = UUIDGenerator.newTimeUUID();

        assertTrue( UUIDComparator.staticCompare( timestamp1, timestamp2 ) < 0 );
        assertTrue( UUIDComparator.staticCompare( timestamp2, timestamp3 ) < 0 );

        //we purposefully write with timestamp2
        serialization.writeEdge( scope, edge1, timestamp2 ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name

        Iterator<MarkedEdge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        Iterator<MarkedEdge> versions = serialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );


        //purposefully write with timestamp1 to ensure this doesn't take, it's a lower uuid
        serialization.deleteEdge( scope, edge1, timestamp1 ).execute();

        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        versions = serialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );


        //should delete
        serialization.deleteEdge( scope, edge1, timestamp2 ).execute();


        //get our edges out by name

        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertFalse( results.hasNext() );

        versions = serialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, "edge", edge1.getTargetNode(), now, null ) );


        assertFalse( versions.hasNext() );

        //write with v3, should exist
        serialization.writeEdge( scope, edge1, timestamp3 ).execute();

        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        versions = serialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );
    }


    /**
     * Tests writing 2 edges quickly in succession, then returning them. Was failing for commitlog impl
     */
    @Test
    public void successiveWriteReturnTarget() throws ConnectionException {
        final MarkedEdge edge1 = createMarkedEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();


        final UUID timestamp1 = UUIDGenerator.newTimeUUID();
        final UUID timestamp2 = UUIDGenerator.newTimeUUID();
        final UUID timestamp3 = UUIDGenerator.newTimeUUID();

        assertTrue( UUIDComparator.staticCompare( timestamp1, timestamp2 ) < 0 );
        assertTrue( UUIDComparator.staticCompare( timestamp2, timestamp3 ) < 0 );

        //we purposefully write with timestamp2
        serialization.writeEdge( scope, edge1, timestamp2 ).execute();


        long now = System.currentTimeMillis();

        //get our edges out by name

        Iterator<MarkedEdge> results =
                serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        Iterator<MarkedEdge> versions = serialization.getEdgeVersions( scope,
                createGetByEdge( edge1.getSourceNode(), "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );


        //purposefully write with timestamp1 to ensure this doesn't take, it's a lower uuid
        serialization.deleteEdge( scope, edge1, timestamp1 ).execute();

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        versions = serialization.getEdgeVersions( scope,
                createGetByEdge( edge1.getSourceNode(), "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );


        //should delete
        serialization.deleteEdge( scope, edge1, timestamp2 ).execute();


        //get our edges out by name

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, null ) );

        assertFalse( results.hasNext() );

        versions = serialization.getEdgeVersions( scope,
                createGetByEdge( edge1.getSourceNode(), "edge", edge1.getTargetNode(), now, null ) );


        assertFalse( versions.hasNext() );

        //write with v3, should exist
        serialization.writeEdge( scope, edge1, timestamp3 ).execute();

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertFalse( results.hasNext() );


        versions = serialization.getEdgeVersions( scope,
                createGetByEdge( edge1.getSourceNode(), "edge", edge1.getTargetNode(), now, null ) );


        assertEquals( edge1, versions.next() );
        assertFalse( versions.hasNext() );
    }


    @Test
    public void testColumnTimestamps() throws ConnectionException {


        ColumnFamily<String, String> cf = new ColumnFamily<>( "test", StringSerializer.get(), StringSerializer.get() );

        if ( keyspace.describeKeyspace().getColumnFamily( "test" ) == null ) {
            keyspace.createColumnFamily( cf, new HashMap<String, Object>() );
        }


        final String rowKey = "test";
        final String colName = "colName" + UUID.randomUUID();
        final long timestamp = 100l;

        MutationBatch batch = keyspace.prepareMutationBatch().withConsistencyLevel( ConsistencyLevel.CL_QUORUM )
                                      .setTimestamp( timestamp );


        batch.withRow( cf, rowKey ).putColumn( colName, true );

        batch.execute();


        Column<String> column = keyspace.prepareQuery( cf ).getKey( rowKey ).getColumn( colName ).execute().getResult();


        assertEquals( colName, column.getName() );
        assertTrue( column.getBooleanValue() );

        //now, delete with the same timestamp
        batch = keyspace.prepareMutationBatch().withConsistencyLevel( ConsistencyLevel.CL_QUORUM )
                        .setTimestamp( timestamp );

        batch.withRow( cf, rowKey ).deleteColumn( colName );
        batch.execute();


        try {
            column = keyspace.prepareQuery( cf ).getKey( rowKey ).getColumn( colName ).execute().getResult();
            fail( "I shouldn't return a value" );
        }
        catch ( NotFoundException nfe ) {
            //swallow
        }


        //now write it again


        batch = keyspace.prepareMutationBatch().withConsistencyLevel( ConsistencyLevel.CL_QUORUM )
                        .setTimestamp( timestamp );

        batch.withRow( cf, rowKey ).putColumn( colName, true );

        batch.execute();

        try {
            column = keyspace.prepareQuery( cf ).getKey( rowKey ).getColumn( colName ).execute().getResult();
            fail( "I shouldn't return a value" );
        }
        catch ( NotFoundException nfe ) {
            //swallow
        }
    }


    private void verify( Iterator<MarkedEdge> results, int expectedCount ) {
        int count = 0;

        while ( results.hasNext() ) {
            count++;
            results.next();
        }


        assertEquals( "All versions returned", expectedCount, count );
    }


    private class IterableWrapper<T> implements Iterable<T> {

        private final Iterator<T> source;


        private IterableWrapper( final Iterator<T> source ) {this.source = source;}


        @Override
        public Iterator<T> iterator() {
            return source;
        }
    }
}
