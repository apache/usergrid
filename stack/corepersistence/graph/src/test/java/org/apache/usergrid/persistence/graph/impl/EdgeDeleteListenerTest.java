/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.UUID;

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
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListener;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createGetByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createMarkedEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchByEdgeAndId;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchEdge;
import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createSearchIdType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith( ITRunner.class )
@UseModules( { TestGraphModule.class } )
public class EdgeDeleteListenerTest {

    private static final Logger log = LoggerFactory.getLogger( NodeDeleteListenerTest.class );

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    protected EdgeMetadataSerialization edgeMetadataSerialization;

    @Inject
    protected EdgeDeleteListener edgeDeleteListener;


    @Inject
    protected EdgeSerialization storageEdgeSerialization;


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


    @Test
    public void testDeleteIT() throws ConnectionException {

        //write several versions to the commit log

        final Id sourceId = IdGenerator.createId( "source" );
        final String edgeType = "test";
        final Id targetId = IdGenerator.createId( "target" );


        final long edgeTimestamp = 1000l;

        MarkedEdge edgeV1 = createMarkedEdge( sourceId, edgeType, targetId, edgeTimestamp );
        MarkedEdge edgeV2 = createMarkedEdge( sourceId, edgeType, targetId, edgeTimestamp + 1 );
        MarkedEdge edgeV3 = createMarkedEdge( sourceId, edgeType, targetId, edgeTimestamp + 2 );


        storageEdgeSerialization.writeEdge( scope, edgeV1, UUIDGenerator.newTimeUUID() ).execute();
        storageEdgeSerialization.writeEdge( scope, edgeV2, UUIDGenerator.newTimeUUID() ).execute();
        storageEdgeSerialization.writeEdge( scope, edgeV3, UUIDGenerator.newTimeUUID() ).execute();




        //now perform the listener execution
        edgeDeleteListener.receive(scope,  edgeV3, UUIDGenerator.newTimeUUID() ).toBlocking().single();

        //now validate there's nothing in the commit log.
        long now = System.currentTimeMillis();

        /******
         * Ensure everything is removed from the commit log
         */

        /**
         * Search all versions of the edge
         */


        /**
         * Ensure all the edges exist in the permanent storage
         */

        /**
         * Search all versions of the edge
         */

        Iterator<MarkedEdge> edges = storageEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );

        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );


        edges = storageEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        /**
         * Search to target
         */


        edges = storageEdgeSerialization.getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );


        assertEquals( edgeV2, edges.next() );
        assertEquals( edgeV1, edges.next() );
        assertFalse( edges.hasNext() );
    }


    @Test
    public void testDeleteAllIT() throws ConnectionException {

        //write several versions to the commit log

        final Id sourceId = IdGenerator.createId( "source" );
        final String edgeType = "test";
        final Id targetId = IdGenerator.createId( "target" );


        final long timestamp = 1000l;

        MarkedEdge edgeV1 = createMarkedEdge( sourceId, edgeType, targetId, timestamp );
        MarkedEdge edgeV2 = createMarkedEdge( sourceId, edgeType, targetId, timestamp + 1 );
        MarkedEdge edgeV3 = createMarkedEdge( sourceId, edgeType, targetId, timestamp + 2);


        final UUID foobar = UUIDGenerator.newTimeUUID();

        storageEdgeSerialization.writeEdge( scope, edgeV1, foobar ).execute();
        storageEdgeSerialization.writeEdge( scope, edgeV2, foobar ).execute();
        storageEdgeSerialization.writeEdge( scope, edgeV3, foobar ).execute();

        edgeMetadataSerialization.writeEdge( scope, edgeV1 ).execute();
        edgeMetadataSerialization.writeEdge( scope, edgeV2 ).execute();
        edgeMetadataSerialization.writeEdge( scope, edgeV3 ).execute();


        //now perform the listener execution, should only clean up to edge v2

                edgeDeleteListener.receive( scope,  edgeV2, UUIDGenerator.newTimeUUID() )
                                  .toBlocking().single();

       edgeDeleteListener.receive(  scope,edgeV1,  UUIDGenerator.newTimeUUID() )
                                     .toBlocking().single();

        edgeDeleteListener.receive( scope, edgeV3, UUIDGenerator.newTimeUUID() )
                                     .toBlocking().single();



        //now validate there's nothing in the commit log.
        long now = System.currentTimeMillis();

        /******
         * Ensure everything is removed from the commit log
         */



        Iterator<String> edgeTypes =
                edgeMetadataSerialization.getEdgeTypesFromSource( scope, createSearchEdge( sourceId, null ) );

        assertFalse( edgeTypes.hasNext() );


        edgeTypes = edgeMetadataSerialization
                .getEdgeTypesFromSource( scope, createSearchIdType( sourceId, edgeType, null ) );

        assertFalse( edgeTypes.hasNext() );




        /**
         * Search to target
         */


        edgeTypes =
                        edgeMetadataSerialization.getEdgeTypesToTarget( scope, createSearchEdge( targetId, null ) );

                assertFalse( edgeTypes.hasNext() );



        /**
         * Ensure none of the edges exist in the permanent storage
         */

        /**
         * Search all versions of the edge
         */

        Iterator<MarkedEdge> edges = storageEdgeSerialization
                .getEdgeVersions( scope, createGetByEdge( sourceId, edgeType, targetId, now, null ) );


        /**
         * Search from source
         */


        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization
                .getEdgesFromSource( scope, createSearchByEdge( sourceId, edgeType, now, null ) );


        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesFromSourceByTargetType( scope,
                createSearchByEdgeAndId( sourceId, edgeType, now, targetId.getType(), null ) );


        assertFalse( edges.hasNext() );

        /**
         * Search to target
         */


        edges = storageEdgeSerialization.getEdgesToTarget( scope, createSearchByEdge( targetId, edgeType, now, null ) );


        assertFalse( edges.hasNext() );

        edges = storageEdgeSerialization.getEdgesToTargetBySourceType( scope,
                createSearchByEdgeAndId( targetId, edgeType, now, sourceId.getType(), null ) );


        assertFalse( edges.hasNext() );

        //test there's no edge metadata

    }


}
