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

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.safehaus.chop.api.IterationChop;

import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test for use with Judo CHOP to stress test
 */
@IterationChop(iterations = 10, threads = 2)
@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class EdgeSerializationChopTest {

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    @StorageEdgeSerialization
    protected EdgeSerialization serialization;

    protected OrganizationScope scope;


    /**
     * Static UUID so ALL nodes write to this as the source
     */
    private static final UUID ORG_ID = UUID.fromString( "5697ad38-8dd8-11e3-8436-600308a690e3" );


    /**
     * Static UUID so ALL nodes write to this as the source
     */
    private static final UUID SOURCE_NODE_ID = UUID.fromString( "5697ad38-8dd8-11e3-8436-600308a690e2" );


    @Before
    public void setup() {
        scope = mock( OrganizationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( ORG_ID );

        when( scope.getOrganization() ).thenReturn( orgId );
    }


    /**
     * Tests loading elements and retrieving them from the same source
     */
    @Test
    public void mixedEdgeTypes() throws ConnectionException {


        final Id sourceId = createId( SOURCE_NODE_ID, "source" );
        final Id targetId = createId( "target" );


        final Edge edge = createEdge( sourceId, "edge", targetId );

        serialization.writeEdge( scope, edge ).execute();


        UUID now = UUIDGenerator.newTimeUUID();

        //get our edges out by name

        Iterator<MarkedEdge> results =
                serialization.getEdgesFromSource( scope, createSearchByEdge( sourceId, "edge", now, null ) );

        boolean found = false;

        while ( !found && results.hasNext() ) {
            if ( edge.equals( results.next() ) ) {
                found = true;
                break;
            }
        }

        assertTrue( "Found entity", found );
    }
}
