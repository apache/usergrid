package org.apache.usergrid.persistence.graph.serialization;


import java.util.Iterator;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith(JukitoRunner.class)
@UseModules({ TestGraphModule.class })
public class EdgeSerializationTest {

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeSerialization serialization;

    protected OrganizationScope scope;


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void mixedEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge1", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge2", targetId );

        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

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
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge", targetId );


        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

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
        final Edge edgev1 = createEdge( "source", "edge1", "target" );

        final Id sourceId = edgev1.getSourceNode();
        final Id targetId = edgev1.getTargetNode();


        final Edge edgev2 = createEdge( sourceId, "edge1", targetId );

        assertTrue( "Edge version 1 has lower time uuid",
                UUIDComparator.staticCompare( edgev1.getVersion(), edgev2.getVersion() ) < 0 );

        //create edge type 2 to ensure we don't get it in results
        final Edge edgeType2V1 = createEdge( sourceId, "edge2", targetId );

        serialization.writeEdge( scope, edgev1 ).execute();
        serialization.writeEdge( scope, edgev2 ).execute();
        serialization.writeEdge( scope, edgeType2V1 ).execute();

        final UUID now = UUIDGenerator.newTimeUUID();


        SearchByEdge search = createGetByEdge( sourceId, "edge1", targetId, now, null );

        Iterator<MarkedEdge> results = serialization.getEdgeFromSource( scope, search );

        assertEquals( edgev2, results.next() );
        assertEquals( edgev1, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertEquals( edgev2, results.next() );
        assertEquals( edgev1, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );

        //test paging
        search = createGetByEdge( sourceId, "edge1", targetId, now, edgev2 );

        results = serialization.getEdgeFromSource( scope, search );

        assertEquals( edgev1, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertEquals( edgev1, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );

        //test paging
        search = createGetByEdge( sourceId, "edge1", targetId, now, edgev1 );

        results = serialization.getEdgeFromSource( scope, search );

        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertFalse( "No results should be returned", results.hasNext() );

        //max version test

        //test max version
        search = createGetByEdge( sourceId, "edge1", targetId, edgev1.getVersion(), null );

        results = serialization.getEdgeFromSource( scope, search );

        assertEquals( edgev1, results.next() );
        assertFalse( "Max version was honored", results.hasNext() );


        search = createGetByEdge( sourceId, "edge1", targetId, edgev1.getVersion(), null );

        results = serialization.getEdgeToTarget( scope, search );

        assertEquals( edgev1, results.next() );
        assertFalse( "Max version was honored", results.hasNext() );
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void mixedIdTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge", createId( "target2" ) );

        final Id targetId2 = edge2.getTargetNode();

        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

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
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge", createId( "target" ) );

        final Id targetId2 = edge2.getTargetNode();

        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

        //get our edges out by name

        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), edge1 ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), edge2 ) );

        assertFalse( results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), edge1 ) );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), edge2 ) );
        assertFalse( results.hasNext() );
    }


    /**
     * Test paging by resuming the search from the edge
     */
    @Test
    public void delete() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge", createId( "target" ) );

        final Id targetId2 = edge2.getTargetNode();

        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

        //get our edges out by name
        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertEquals( edge2, results.next() );
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

        serialization.deleteEdge( scope, edge1 ).execute();
        serialization.deleteEdge( scope, edge2 ).execute();


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
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();
        final Id targetId1 = edge1.getTargetNode();


        final Edge edge2 = createEdge( sourceId, "edge", createId( "target" ) );

        final Id targetId2 = edge2.getTargetNode();

        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

        //get our edges out by name
        Iterator<MarkedEdge> results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );

        assertEquals( edge1, results.next() );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        assertEquals( edge1, results.next() );
        assertEquals( edge2, results.next() );
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

        serialization.markEdge( scope, edge1 ).execute();
        serialization.markEdge( scope, edge2 ).execute();


        results = serialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, "edge", now, targetId1.getType(), null ) );


        MarkedEdge edge = results.next();

        assertEquals( edge1, edge );
        assertTrue( edge.isDeleted() );


        edge = results.next();

        assertEquals( edge2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        //get them out by type
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        edge = results.next();

        assertEquals( edge1, edge );
        assertTrue( edge.isDeleted() );

        edge = results.next();

        assertEquals( edge2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId1, "edge", now, sourceId.getType(), null ) );

        edge = results.next();

        assertEquals( edge1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        results = serialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId2, "edge", now, sourceId.getType(), null ) );

        edge = results.next();

        assertEquals( edge2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        //validate we get from target
        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId1, "edge", now, null ) );

        edge = results.next();

        assertEquals( edge1, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId2, "edge", now, null ) );

        edge = results.next();

        assertEquals( edge2, edge );
        assertTrue( edge.isDeleted() );

        assertFalse( results.hasNext() );

        //now we've validated everything exists
    }

}
