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
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
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
@UseModules({ GraphModule.class })
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

        Iterator<Edge> results =
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

        Iterator<Edge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, edge1 ) );

        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );

        //test getting the next edge
        results = serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, edge2 ) );

        assertFalse( "No results should be returned", results.hasNext() );

        //test getting source edges from the target

        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, edge1 ) );
        assertEquals( edge2, results.next() );
        assertFalse( results.hasNext() );


        results = serialization.getEdgesToTarget( scope, createSearchByEdge( targetId, "edge", now, edge2 ) );
        assertFalse( "No results should be returned", results.hasNext() );
        //test resume by name
    }


    /**
     * Tests mixing 2 edge types between 2 nodes.  We should get results for the same source->destination with the 2
     * edge types
     */
    @Test
    public void directEdgeGets() throws ConnectionException {
        final Edge edgev1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edgev1.getSourceNode();
        final Id targetId = edgev1.getTargetNode();


        final Edge edgev2 = createEdge( sourceId, "edge", targetId );

        assertTrue( "Edge version 1 has lower time uuid", edgev1.getVersion().compareTo( edgev2.getVersion() ) < 0 );

        serialization.writeEdge( scope, edgev1 ).execute();

        final UUID now = UUIDGenerator.newTimeUUID();


        SearchByEdge search = createGetByEdge( sourceId, "edge1", targetId, now, null );

        Iterator<Edge> results = serialization.getEdgeFromSource( scope, search );

        assertEquals( edgev1, results.next() );
        assertEquals( edgev2, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertEquals( edgev1, results.next() );
        assertEquals( edgev2, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );

        //test paging
        search = createGetByEdge( sourceId, "edge1", targetId, now, edgev1 );

        results = serialization.getEdgeFromSource( scope, search );

        assertEquals( edgev2, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertEquals( edgev2, results.next() );
        assertFalse( "No results should be returned", results.hasNext() );

        //test paging
        search = createGetByEdge( sourceId, "edge1", targetId, now, edgev2 );

        results = serialization.getEdgeFromSource( scope, search );

        assertFalse( "No results should be returned", results.hasNext() );


        results = serialization.getEdgeToTarget( scope, search );

        assertFalse( "No results should be returned", results.hasNext() );
    }
}
