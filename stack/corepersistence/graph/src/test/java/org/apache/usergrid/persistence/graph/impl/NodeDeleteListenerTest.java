package org.apache.usergrid.persistence.graph.impl;


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
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchIdType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
public class NodeDeleteListenerTest {

    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected NodeDeleteListener deleteListener;

    @Inject
    protected EdgeSerialization edgeSerialization;

    @Inject
    protected EdgeMetadataSerialization edgeMetadataSerialization;

    @Inject
    protected NodeSerialization nodeSerialization;

    @Inject
    protected EdgeManagerFactory emf;


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
     * Simple test case that tests a single edge and removing the node.  The other target node should be removed as well
     * since it has no other targets
     */
    @Test
    public void testNoDeletionMarked() {

        EdgeManager em = emf.createEdgeManager( scope );

        Edge edge = createEdge( "source", "test", "target" );

        //write the edge
        Edge last = em.writeEdge( edge ).toBlockingObservable().last();

        assertEquals( edge, last );

        Id sourceNode = edge.getSourceNode();

        Id targetNode = edge.getTargetNode();

        UUID version = UUIDGenerator.newTimeUUID();


        EdgeEvent<Id> deleteEvent = new EdgeEvent<Id>( scope, version, sourceNode );

        EdgeEvent<Id> event = deleteListener.receive( deleteEvent ).toBlockingObservable().lastOrDefault( null );

        assertNull( "Mark was not set, no delete should be executed", event );


        deleteEvent = new EdgeEvent<Id>( scope, version, targetNode );

        event = deleteListener.receive( deleteEvent ).toBlockingObservable().lastOrDefault( null );

        assertNull( "Mark was not set, no delete should be executed", event );
    }


    /**
     * Simple test case that tests a single edge and removing the node.  The other target node should be removed as well
     * since it has no other targets
     */
    @Test
    public void testRemoveSourceNode() throws ConnectionException {

        EdgeManager em = emf.createEdgeManager( scope );

        Edge edge = createEdge( "source", "test", "target" );

        //write the edge
        Edge last = em.writeEdge( edge ).toBlockingObservable().last();


        assertEquals( edge, last );

        Id sourceNode = edge.getSourceNode();

        Id targetNode = edge.getTargetNode();


        //mark the node so
        UUID deleteVersion = UUIDGenerator.newTimeUUID();

        nodeSerialization.mark( scope, sourceNode, deleteVersion ).execute();

        EdgeEvent<Id> deleteEvent = new EdgeEvent<Id>( scope, deleteVersion, sourceNode );


        EdgeEvent<Id> event = deleteListener.receive( deleteEvent ).toBlockingObservable().last();

        assertEquals( deleteEvent, event );

        //now verify we can't get any of the info back

        UUID now = UUIDGenerator.newTimeUUID();


        Iterator<MarkedEdge> returned = edgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceNode, edge.getType(), now, null ) );

        //no edge from source node should be returned
        assertFalse( "No source should be returned", returned.hasNext() );

        //validate it's not returned by the

        returned = edgeSerialization
                .getEdgesToTarget( scope, createSearchByEdge( targetNode, edge.getType(), now, null ) );

        assertFalse( "No target should be returned", returned.hasNext() );

        //no types from source

        Iterator<String> types =
                edgeMetadataSerialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceNode, null ) );

        assertFalse( types.hasNext() );

        //no types to target

        types = edgeMetadataSerialization.getEdgeTypesToTarget( scope, createSearchEdge( targetNode, null ) );

        assertFalse( types.hasNext() );


        //no target types from source

        Iterator<String> idTypes = edgeMetadataSerialization
                .getIdTypesFromSource( scope, createSearchIdType( sourceNode, edge.getType(), null ) );

        assertFalse( idTypes.hasNext() );

        //no source types to target
        idTypes = edgeMetadataSerialization
                .getIdTypesToTarget( scope, createSearchIdType( targetNode, edge.getType(), null ) );

        assertFalse( idTypes.hasNext() );
    }


    /**
     * Simple test case that tests a single edge and removing the node.  The  source node should be removed as well
     * since it has no other targets
     */
    @Test
    public void testRemoveTargetNode() {

    }
}
