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


import java.util.Iterator;

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
import org.apache.usergrid.persistence.graph.guice.GraphModule;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
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
@RunWith( JukitoRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeMetadataSerializationTest {


    @ClassRule
    public static CassandraRule rule = new CassandraRule();


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected EdgeMetadataSerialization serialization;

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
     * Test write and read edge types from source -> target
     */
    @Test
    public void readTargetEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", createId( "target2" ) );

        final Edge edge3 = createEdge( sourceId, "edge2", createId( "target3" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //now check we can resume correctly with a "last"

        edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, "edge" ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void readSourceEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( createId( "source2" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //now check we can resume correctly with a "last"

        edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, "edge" ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void readTargetEdgeIdTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", createId( "target" ) );

        final Edge edge3 = createEdge( sourceId, "edge", createId( "target2" ) );

        //shouldn't be returned
        final Edge edge4 = createEdge( sourceId, "edge2", createId( "target3" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();
        serialization.writeEdge( scope, edge4 ).execute();

        //now check we get both types back
        Iterator<String> types = serialization.getTargetIdTypes( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", types.next() );
        assertEquals( "target2", types.next() );
        assertFalse( types.hasNext() );


        //now check we can resume correctly with a "last"

        types = serialization.getTargetIdTypes( scope, createSearchIdType( sourceId, "edge", "target" ) );

        assertEquals( "target2", types.next() );
        assertFalse( types.hasNext() );


        //check by other type
        types = serialization.getTargetIdTypes( scope, createSearchIdType( sourceId, "edge2", null ) );
        assertEquals( "target3", types.next() );
        assertFalse( types.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void readSourceEdgeIdTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( createId( "source2" ), "edge", targetId );

        //shouldn't be returned
        final Edge edge4 = createEdge( createId( "source3" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();
        serialization.writeEdge( scope, edge4 ).execute();

        //now check we get both types back
        Iterator<String> types = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", types.next() );
        assertEquals( "source2", types.next() );
        assertFalse( types.hasNext() );


        //now check we can resume correctly with a "last"

        types = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge", "source" ) );

        assertEquals( "source2", types.next() );
        assertFalse( types.hasNext() );


        //check by other type
        types = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge2", null ) );
        assertEquals( "source3", types.next() );
        assertFalse( types.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteTargetEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", createId( "target2" ) );

        final Edge edge3 = createEdge( sourceId, "edge2", createId( "target3" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeTargetEdgeType( scope, edge1 ).execute();

        edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this should delete it. The version is the max for that edge type
        serialization.removeTargetEdgeType( scope, edge2 ).execute();


        //now check we have 1 left
        edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeTargetEdgeType( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getTargetEdgeTypes( scope, createSearchEdge( sourceId, null ) );

        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteSourceEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( createId( "source2" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();


        //now check we get both types back
        Iterator<String> edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeTargetEdgeType( scope, edge1 ).execute();

        edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );


        serialization.removeSourceEdgeType( scope, edge2 ).execute();

        //now check we have 1 left
        edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeSourceEdgeType( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getSourceEdgeTypes( scope, createSearchEdge( targetId, null ) );

        assertFalse( edges.hasNext() );
    }



    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteTargetIdTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", createId( "target" ) );

        final Edge edge3 = createEdge( sourceId, "edge", createId( "target2" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getTargetIdTypes( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", edges.next() );
        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeTargetIdType( scope, edge1 ).execute();

        edges = serialization.getTargetIdTypes( scope,  createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", edges.next() );
        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        //this should delete it. The version is the max for that edge type
        serialization.removeTargetIdType( scope, edge2 ).execute();


        //now check we have 1 left
        edges = serialization.getTargetIdTypes( scope,  createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeTargetIdType( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getTargetIdTypes( scope,  createSearchIdType( sourceId, "edge", null ));

        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteSourceIdTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( createId( "source2" ), "edge", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();


        //now check we get both types back
        Iterator<String> edges = serialization.getSourceIdTypes( scope,  createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", edges.next() );
        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeSourceIdType( scope, edge1 ).execute();

        edges = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", edges.next() );
        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );


        serialization.removeSourceIdType( scope, edge2 ).execute();

        //now check we have 1 left
        edges = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeSourceIdType( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getSourceIdTypes( scope, createSearchIdType( targetId, "edge", null ) );

        assertFalse( edges.hasNext() );
    }
}
