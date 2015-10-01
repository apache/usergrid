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
package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListener;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchIdType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class NodeDeleteListenerTest {

    private static final Logger log = LoggerFactory.getLogger( NodeDeleteListenerTest.class );


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
    protected GraphManagerFactory emf;

    @Inject
    protected GraphFig graphFig;


    protected ApplicationScope scope;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );
    }


    /**
     * Simple test case that tests a single edge and removing the node.  The other target node should be removed as well
     * since it has no other targets
     */
    @Test
    public void testNoDeletionMarked() {

        GraphManager em = emf.createEdgeManager( scope );

        Edge edge = createEdge( "source", "test", "target" );

        //write the edge
        Edge last = em.writeEdge( edge ).toBlocking().last();

        assertEquals( edge, last );

        Id sourceNode = edge.getSourceNode();


        UUID eventTime = UUIDGenerator.newTimeUUID();


        int count = deleteListener.receive( scope, sourceNode, eventTime ).toBlocking().last();

        assertEquals( "Mark was not set, no delete should be executed", 0, count );


    }


    /**
     * Simple test case that tests a single edge and removing the node.  The other target node should be removed as well
     * since it has no other targets
     */
    @Test
    public void testRemoveSourceNode() throws ConnectionException {

        GraphManager em = emf.createEdgeManager( scope );

        Edge edge = createEdge( "source", "test", "target" );

        //write the edge
        Edge last = em.writeEdge( edge ).toBlocking().last();


        assertEquals( edge, last );

        Id sourceNode = edge.getSourceNode();

        Id targetNode = edge.getTargetNode();


        //mark the node so
        UUID deleteEventTimestamp = UUIDGenerator.newTimeUUID();
        long timestamp = System.currentTimeMillis();

        nodeSerialization.mark( scope, sourceNode, timestamp ).execute();

        int count = deleteListener.receive( scope, sourceNode, deleteEventTimestamp ).toBlocking().last();

        assertEquals( 1, count );

        //now verify we can't get any of the info back

        long now = System.currentTimeMillis();


        Iterator<MarkedEdge> returned = edgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceNode, edge.getType(), now, null ) );

        //no edge from source node should be returned
        assertFalse( "No source should be returned", returned.hasNext() );

        //validate it's not returned by the

        returned = edgeSerialization
                .getEdgesToTarget( scope, createSearchByEdge( targetNode, edge.getType(), now, null ) );

        assertFalse( "No target should be returned", returned.hasNext() );


        returned = edgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceNode, edge.getType(), targetNode, now, null ) );

        assertFalse( "No version should be returned", returned.hasNext() );

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
    public void testRemoveTargetNode() throws ConnectionException {

        GraphManager em = emf.createEdgeManager( scope );

        Edge edge = createEdge( "source", "test", "target" );

        //write the edge
        Edge last = em.writeEdge( edge ).toBlocking().last();


        assertEquals( edge, last );

        Id sourceNode = edge.getSourceNode();

        Id targetNode = edge.getTargetNode();


        //mark the node so
        final long deleteBefore = System.currentTimeMillis();

        nodeSerialization.mark( scope, targetNode, deleteBefore ).execute();

        int count = deleteListener.receive( scope, targetNode, UUIDGenerator.newTimeUUID() ).toBlocking().last();

        assertEquals( 1, count );

        //now verify we can't get any of the info back

        long now = System.currentTimeMillis();


        Iterator<MarkedEdge> returned = edgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceNode, edge.getType(), now, null ) );

        //no edge from source node should be returned
        assertFalse( "No source should be returned", returned.hasNext() );

        //validate it's not returned by the

        returned = edgeSerialization
                .getEdgesToTarget( scope, createSearchByEdge( targetNode, edge.getType(), now, null ) );

        assertFalse( "No target should be returned", returned.hasNext() );


        returned = edgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceNode, edge.getType(), targetNode, now, null ) );

        assertFalse( "No version should be returned", returned.hasNext() );

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
     * Simple test case that tests a single edge and removing the node.  The other target node should be removed as well
     * since it has no other targets
     */
    @Test
    @Ignore("This needs to be re-enable.  The counters for sharding fall over in cass, needs fixes")
    public void testMultiDelete() throws ConnectionException, InterruptedException {

        GraphManager em = emf.createEdgeManager( scope );


        //create loads of edges to easily delete.  We'll keep all the types of "test"
        final int edgeCount = graphFig.getScanPageSize() * 2;
        Id toDelete = IdGenerator.createId( "toDelete" );
        final String edgeType = "test";

        int countSaved = 0;
        int sourceCount = 0;
        int targetCount = 0;


        for ( int i = 0; i < edgeCount; i++ ) {
            Edge edge;

            //mix up source vs target, good for testing as well as create a lot of sub types to ensure they're removed
            if ( i % 2 == 0 ) {
                edge = createEdge( toDelete, edgeType, IdGenerator.createId( "target" + Math.random() ) );
                sourceCount++;
            }
            else {
                edge = createEdge( IdGenerator.createId( "source" + Math.random() ), edgeType, toDelete );
                targetCount++;
            }

            //write the edge
            Edge last = em.writeEdge( edge ).toBlocking().last();


            assertEquals( edge, last );

            countSaved++;
        }

        assertEquals( edgeCount, countSaved );

        log.info( "Saved {} source edges", sourceCount );
        log.info( "Saved {} target edges", targetCount );

        long deleteVersion = Long.MAX_VALUE;

        nodeSerialization.mark( scope, toDelete, deleteVersion ).execute();

        int count = deleteListener.receive( scope, toDelete, UUIDGenerator.newTimeUUID() ).toBlocking().last();

        //TODO T.N. THIS SHOULD WORK!!!!  It fails intermittently with RX 0.17.1 with too many scheduler threads (which was wrong), try this again after the next release
        assertEquals( edgeCount, count );

        //now verify we can't get any of the info back

        long now = System.currentTimeMillis();


           //validate it's not returned by the

        Iterator<MarkedEdge>  returned = edgeSerialization.getEdgesToTarget( scope, createSearchByEdge( toDelete, edgeType, now, null ) );

        assertFalse( "No target should be returned", returned.hasNext() );

        returned =
                edgeSerialization.getEdgesFromSource( scope, createSearchByEdge( toDelete, edgeType, now, null ) );

        //no edge from source node should be returned
        assertFalse( "No source should be returned", returned.hasNext() );




        //no types from source

        Iterator<String> types =
                edgeMetadataSerialization.getEdgeTypesFromSource( scope, createSearchEdge( toDelete, null ) );

        assertFalse( types.hasNext() );


        //no types to target

        types = edgeMetadataSerialization.getEdgeTypesToTarget( scope, createSearchEdge( toDelete, null ) );

        assertFalse( types.hasNext() );


        //no target types from source

        Iterator<String> idTypes =
                edgeMetadataSerialization.getIdTypesFromSource( scope, createSearchIdType( toDelete, edgeType, null ) );

        assertFalse( idTypes.hasNext() );

        //no source types to target
        idTypes = edgeMetadataSerialization.getIdTypesToTarget( scope, createSearchIdType( toDelete, edgeType, null ) );

        assertFalse( idTypes.hasNext() );
    }
}
