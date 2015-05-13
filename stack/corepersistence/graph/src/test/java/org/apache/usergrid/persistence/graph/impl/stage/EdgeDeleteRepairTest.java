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

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.graph.Edge;
import org.junit.Before;
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
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
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
    protected EdgeSerialization storageEdgeSerialization;

    @Inject
    protected EdgeDeleteRepair edgeDeleteRepair;


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
     * Test repairing with no edges
     */
    @Test
    public void noEdges() {
        MarkedEdge edge = createEdge( "source", "test", "target" );

        Iterator<MarkedEdge> edges = edgeDeleteRepair.repair( scope, edge, UUIDGenerator.newTimeUUID() ).toBlocking().getIterator();

        assertFalse( "No edges cleaned", edges.hasNext() );
    }


    /**
     * Commit log tests
     */
    @Test
    public void commitLogTest() throws ConnectionException {


        final Id sourceId = IdGenerator.createId( "source" );
        final Id targetId = IdGenerator.createId( "target" );
        final String edgeType = "edge";


        final MarkedEdge edge1 = createEdge( sourceId, edgeType, targetId, System.currentTimeMillis(), true );

        //write it as non deleted to storage

        storageEdgeSerialization.writeEdge( scope, edge1,  UUIDGenerator.newTimeUUID() ).execute();


        final MarkedEdge edge2 = createEdge( sourceId, edgeType, targetId );
        storageEdgeSerialization.writeEdge( scope, edge2, UUIDGenerator.newTimeUUID() ).execute();

        //now repair delete the first edge


        Iterator<MarkedEdge> itr = storageEdgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING, Optional.<Edge>absent() ) );

        assertEquals( edge2, itr.next() );
        assertEquals( edge1, itr.next() );
        assertFalse( itr.hasNext() );

        MarkedEdge deleted = edgeDeleteRepair.repair( scope, edge1, UUIDGenerator.newTimeUUID() ).toBlocking().single();

        assertEquals( edge1, deleted );

        itr = storageEdgeSerialization.getEdgeVersions( scope,
                new SimpleSearchByEdge( sourceId, edgeType, targetId, System.currentTimeMillis(), SearchByEdgeType.Order.DESCENDING,  Optional.<Edge>absent() ) );

        assertEquals( edge2, itr.next() );
        assertFalse( itr.hasNext() );
    }


    /**
     * If the edge is NOT marked as deleted in the commit log, then we don't want to
     */
    @Test
    public void notDeletedInCommitLog() {

    }
}
