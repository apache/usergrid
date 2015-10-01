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
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.Edge;
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
import com.netflix.astyanax.serializers.StringSerializer;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchIdType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Made abstract to allow subclasses to perform the wiring required for the functional testing.
 */
public abstract class EdgeMetadataSerializationTest {



    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    protected Keyspace keyspace;


    protected ApplicationScope scope;

    protected EdgeMetadataSerialization serialization;


    @Before
    public void setup() {
        scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( scope.getApplication() ).thenReturn( orgId );

        serialization = getSerializationImpl();
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void readTargetEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target2" ) );

        final Edge edge3 = createEdge( sourceId, "edge2", IdGenerator.createId( "target3" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //now check we can resume correctly with a "last"

        edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, "edge" ) );

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

        final Edge edge2 = createEdge( IdGenerator.createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( IdGenerator.createId( "source2" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //now check we can resume correctly with a "last"

        edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, "edge" ) );

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

        final Edge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target" ) );

        final Edge edge3 = createEdge( sourceId, "edge", IdGenerator.createId( "target2" ) );

        //shouldn't be returned
        final Edge edge4 = createEdge( sourceId, "edge2", IdGenerator.createId( "target3" ) );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();
        serialization.writeEdge( scope, edge4 ).execute();

        //now check we get both types back
        Iterator<String> types =
                serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", types.next() );
        assertEquals( "target2", types.next() );
        assertFalse( types.hasNext() );


        //now check we can resume correctly with a "last"

        types = serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", "target" ) );

        assertEquals( "target2", types.next() );
        assertFalse( types.hasNext() );


        //check by other type
        types = serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge2", null ) );
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

        final Edge edge2 = createEdge( IdGenerator.createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( IdGenerator.createId( "source2" ), "edge", targetId );

        //shouldn't be returned
        final Edge edge4 = createEdge( IdGenerator.createId( "source3" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();
        serialization.writeEdge( scope, edge4 ).execute();

        //now check we get both types back
        Iterator<String> types =
                serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", types.next() );
        assertEquals( "source2", types.next() );
        assertFalse( types.hasNext() );


        //now check we can resume correctly with a "last"

        types = serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", "source" ) );

        assertEquals( "source2", types.next() );
        assertFalse( types.hasNext() );


        //check by other type
        types = serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge2", null ) );
        assertEquals( "source3", types.next() );
        assertFalse( types.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteTargetEdgeTypes() throws ConnectionException {
        final long timestamp = 1000l;
        final Edge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target2" ), timestamp + 1 );

        final Edge edge3 = createEdge( sourceId, "edge2", IdGenerator.createId( "target3" ), timestamp + 2 );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeEdgeTypeFromSource( scope, edge1 ).execute();

        edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this should delete it. The version is the max for that edge type
        serialization.removeEdgeTypeFromSource( scope, edge2 ).execute();


        //now check we have 1 left
        edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeEdgeTypeFromSource( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteSourceEdgeTypes() throws ConnectionException {
        final Edge edge1 = createEdge( "source", "edge", "target" );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( IdGenerator.createId( "source" ), "edge", targetId );

        final Edge edge3 = createEdge( IdGenerator.createId( "source2" ), "edge2", targetId );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();


        //now check we get both types back
        Iterator<String> edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeEdgeTypeFromSource( scope, edge1 ).execute();

        edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge", edges.next() );
        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );


        serialization.removeEdgeTypeToTarget( scope, edge2 ).execute();

        //now check we have 1 left
        edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

        assertEquals( "edge2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeEdgeTypeToTarget( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteTargetIdTypes() throws ConnectionException {

        final long timestamp = 1000l;

        final Edge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id sourceId = edge1.getSourceNode();

        final Edge edge2 = createEdge( sourceId, "edge", IdGenerator.createId( "target" ), timestamp + 1 );

        final Edge edge3 = createEdge( sourceId, "edge", IdGenerator.createId( "target2" ), timestamp + 2 );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();

        //now check we get both types back
        Iterator<String> edges =
                serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", edges.next() );
        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeIdTypeFromSource( scope, edge1 ).execute();

        edges = serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target", edges.next() );
        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        //this should delete it. The version is the max for that edge type
        serialization.removeIdTypeFromSource( scope, edge2 ).execute();


        //now check we have 1 left
        edges = serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", null ) );

        assertEquals( "target2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeIdTypeFromSource( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getIdTypesFromSource( scope, createSearchIdType( sourceId, "edge", null ) );

        assertFalse( edges.hasNext() );
    }


    /**
     * Test write and read edge types from source -> target
     */
    @Test
    public void deleteSourceIdTypes() throws ConnectionException {

        final long timestamp = 1000l;

        final Edge edge1 = createEdge( "source", "edge", "target", timestamp );

        final Id targetId = edge1.getTargetNode();

        final Edge edge2 = createEdge( IdGenerator.createId( "source" ), "edge", targetId, timestamp + 1 );

        final Edge edge3 = createEdge( IdGenerator.createId( "source2" ), "edge", targetId, timestamp + 2 );

        //set writing the edge
        serialization.writeEdge( scope, edge1 ).execute();
        serialization.writeEdge( scope, edge2 ).execute();
        serialization.writeEdge( scope, edge3 ).execute();


        //now check we get both types back
        Iterator<String> edges =
                serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", edges.next() );
        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );

        //this shouldn't remove the edge, since edge1 has a version < edge2
        serialization.removeIdTypeToTarget( scope, edge1 ).execute();

        edges = serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source", edges.next() );
        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );


        serialization.removeIdTypeToTarget( scope, edge2 ).execute();

        //now check we have 1 left
        edges = serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", null ) );

        assertEquals( "source2", edges.next() );
        assertFalse( edges.hasNext() );

        serialization.removeIdTypeToTarget( scope, edge3 ).execute();

        //check we have nothing
        edges = serialization.getIdTypesToTarget( scope, createSearchIdType( targetId, "edge", null ) );

        assertFalse( edges.hasNext() );
    }


    @Test
    public void validateDeleteCollision() throws ConnectionException {


        final String CF_NAME = "test";
        final StringSerializer STR_SER = StringSerializer.get();


        ColumnFamily<String, String> testCf = new ColumnFamily<String, String>( CF_NAME, STR_SER, STR_SER );

        if ( keyspace.describeKeyspace().getColumnFamily( CF_NAME ) == null ) {
            keyspace.createColumnFamily( testCf, null );
        }


        final String key = "key";
        final String colname = "name";
        final String colvalue = "value";

        UUID firstUUID = UUIDGenerator.newTimeUUID();

        UUID secondUUID = UUIDGenerator.newTimeUUID();

        UUID thirdUUID = UUIDGenerator.newTimeUUID();

        assertTrue( "First before second", UUIDComparator.staticCompare( firstUUID, secondUUID ) < 0 );

        assertTrue( "Second before third", UUIDComparator.staticCompare( secondUUID, thirdUUID ) < 0 );

        MutationBatch batch = keyspace.prepareMutationBatch();

        batch.withRow( testCf, key ).setTimestamp( firstUUID.timestamp() ).putColumn( colname, colvalue );

        batch.execute();

        //now read it back to validate

        Column<String> col = keyspace.prepareQuery( testCf ).getKey( key ).getColumn( colname ).execute().getResult();

        assertEquals( colname, col.getName() );
        assertEquals( colvalue, col.getStringValue() );

        //now issue a write and a delete with the same timestamp, write will win

        batch = keyspace.prepareMutationBatch();
        batch.withRow( testCf, key ).setTimestamp( firstUUID.timestamp() ).putColumn( colname, colvalue );
        batch.withRow( testCf, key ).setTimestamp( firstUUID.timestamp() ).deleteColumn( colname );
        batch.execute();

        boolean deleted = false;

        try {
            keyspace.prepareQuery( testCf ).getKey( key ).getColumn( colname ).execute().getResult();
        }
        catch ( NotFoundException nfe ) {
            deleted = true;
        }

        assertTrue( deleted );

        //ensure that if we have a latent write, it won't overwrite a newer value
        batch.withRow( testCf, key ).setTimestamp( secondUUID.timestamp() ).putColumn( colname, colvalue );
        batch.execute();

        col = keyspace.prepareQuery( testCf ).getKey( key ).getColumn( colname ).execute().getResult();

        assertEquals( colname, col.getName() );
        assertEquals( colvalue, col.getStringValue() );

        //now issue a delete with the first timestamp, column should still be present
        batch = keyspace.prepareMutationBatch();
        batch.withRow( testCf, key ).setTimestamp( firstUUID.timestamp() ).deleteColumn( colname );
        batch.execute();


        col = keyspace.prepareQuery( testCf ).getKey( key ).getColumn( colname ).execute().getResult();

        assertEquals( colname, col.getName() );
        assertEquals( colvalue, col.getStringValue() );

        //now delete it with the 3rd timestamp, it should disappear

        batch = keyspace.prepareMutationBatch();
        batch.withRow( testCf, key ).setTimestamp( thirdUUID.timestamp() ).deleteColumn( colname );
        batch.execute();

        deleted = false;

        try {
            keyspace.prepareQuery( testCf ).getKey( key ).getColumn( colname ).execute().getResult();
        }
        catch ( NotFoundException nfe ) {
            deleted = true;
        }

        assertTrue( deleted );
    }


    protected abstract EdgeMetadataSerialization getSerializationImpl();
}
