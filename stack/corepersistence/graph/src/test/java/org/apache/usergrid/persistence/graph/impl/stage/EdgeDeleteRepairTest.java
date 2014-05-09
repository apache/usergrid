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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.Iterator;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
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
public class EdgeDeleteRepairTest {

    private static final Logger LOG = LoggerFactory.getLogger( EdgeDeleteRepairTest.class );


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    @CommitLogEdgeSerialization
    protected EdgeSerialization commitLogEdgeSerialization;


    @Inject
    @StorageEdgeSerialization
    protected EdgeSerialization storageEdgeSerialization;

    @Inject
    protected EdgeDeleteRepair edgeDeleteRepair;


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
     * Test repairing with no edges
     */
    @Test
    public void noEdges() {
        Edge edge = createEdge( "source", "test", "target" );

        Iterator<MarkedEdge> edges = edgeDeleteRepair.repair( scope, edge ).toBlockingObservable().getIterator();

        assertFalse( "No edges cleaned", edges.hasNext() );
    }


    /**
     * Commit log tests
     */
    @Test
    public void commitLogTest() throws ConnectionException {
        testSingleLocation( commitLogEdgeSerialization );
    }


    /**
     * Commit log tests
     */
    @Test
    public void storageTest() throws ConnectionException {
        testSingleLocation( storageEdgeSerialization );
    }


    private void testSingleLocation( EdgeSerialization edgeSerialization ) throws ConnectionException {

        final Id sourceId = createId( "source" );
        final Id targetId = createId( "target" );
        final String edgeType = "edge";


        final Edge edge1 = createEdge( sourceId, edgeType, targetId );
        edgeSerialization.writeEdge( scope, edge1 ).execute();

        final Edge edge2 = createEdge( sourceId, edgeType, targetId );
        edgeSerialization.writeEdge( scope, edge2 ).execute();

        //now repair delete the first edge

        MarkedEdge deleted = edgeDeleteRepair.repair( scope, edge1 ).toBlockingObservable().single();

        assertEquals( edge1, deleted );

        Iterator<MarkedEdge> itr = edgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID(), null ) );

        assertEquals( edge2, itr.next() );

        assertFalse( itr.hasNext() );
    }


    /**
     * Tests the edge in both
     * @throws ConnectionException
     */
    @Test
    public void testBoth() throws ConnectionException {

        final Id sourceId = createId( "source" );
        final Id targetId = createId( "target" );
        final String edgeType = "edge";


        final Edge edge1 = createEdge( sourceId, edgeType, targetId );
        commitLogEdgeSerialization.writeEdge( scope, edge1 ).execute();
        storageEdgeSerialization.writeEdge( scope, edge1 ).execute();

        final Edge edge2 = createEdge( sourceId, edgeType, targetId );
        commitLogEdgeSerialization.writeEdge( scope, edge2 ).execute();
        storageEdgeSerialization.writeEdge( scope, edge2 ).execute();

        //now repair delete the first edge

        MarkedEdge deleted = edgeDeleteRepair.repair( scope, edge1 ).toBlockingObservable().single();

        assertEquals( edge1, deleted );

        Iterator<MarkedEdge> itr = commitLogEdgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID(), null ) );

        assertEquals( edge2, itr.next() );

        assertFalse( itr.hasNext() );

        itr = storageEdgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, UUIDGenerator.newTimeUUID(), null ) );

        assertEquals( edge2, itr.next() );

        assertFalse( itr.hasNext() );
    }
}
