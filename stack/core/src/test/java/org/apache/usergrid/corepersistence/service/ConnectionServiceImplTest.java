/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.service;


import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.TestCoreModule;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith( ITRunner.class )
@UseModules( TestCoreModule.class )
public class ConnectionServiceImplTest {


    private static final Logger logger = LoggerFactory.getLogger(ConnectionServiceImplTest.class);

    @Inject
    private GraphManagerFactory graphManagerFactory;

    @Inject
    private ConnectionService connectionService;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Test
    public void testSingleConnection() {

        final ApplicationScope applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );


        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );

        //now write a single connection

        final Id source = new SimpleId( "source" );


        //add to a collection

        final String collectionName = "testCollection";

        final Edge collectionEdge = CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), collectionName, source );

        final Edge writtenCollection = gm.writeEdge( collectionEdge ).toBlocking().last();

        assertNotNull("Collection edge written", writtenCollection);




        final Id target = new SimpleId( "target" );

        final String connectionType = "testConnection";

        final Edge connectionEdge = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge writtenConnection = gm.writeEdge( connectionEdge ).toBlocking().last();


        //now run the cleanup

        final int count =
            connectionService.deDupeConnections( Observable.just( applicationScope ) ).count().toBlocking().last();

        assertEquals( "No edges deleted", 0, count );

        //now ensure we can read the edge.

        final SearchByEdge simpleSearchByEdge =
            new SimpleSearchByEdge( source, connectionEdge.getType(), target, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.absent() );

        final List<Edge> edges = gm.loadEdgeVersions( simpleSearchByEdge ).toList().toBlocking().last();

        assertEquals( 1, edges.size() );

        assertEquals( writtenConnection, edges.get( 0 ) );
    }


    @Test
    public void testDuplicateConnections() {

        final ApplicationScope applicationScope = new ApplicationScopeImpl( new SimpleId( "application" ) );


        final GraphManager gm = graphManagerFactory.createEdgeManager( applicationScope );

        //now write a single connection

        final Id source = new SimpleId( "source" );



          //add to a collection

        final String collectionName = "testCollection";

        final Edge collectionEdge = CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), collectionName, source );

        final Edge writtenCollection = gm.writeEdge( collectionEdge ).toBlocking().last();

        assertNotNull("Collection edge written", writtenCollection);


        //now write 3 connections between the same nodes.

        final Id target = new SimpleId( "target" );

        final String connectionType = "testConnection";



        //write our first connection
        final Edge connection1 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written1 = gm.writeEdge( connection1 ).toBlocking().last();

        logger.info( "Wrote edge 1 with edge {}", written1 );


        //write the second
        final Edge connection2 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written2 = gm.writeEdge( connection2 ).toBlocking().last();

        logger.info( "Wrote edge 2 with edge {}", written2 );


        //write the 3rd
        final Edge connection3 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written3 = gm.writeEdge( connection3 ).toBlocking().last();


        logger.info( "Wrote edge 3 with edge {}", written3 );



        assertTrue( "Expected edge timestamp to be in order", written1.getTimestamp() <= written2.getTimestamp() );
        assertTrue( "Expected edge timestamp to be in order", written2.getTimestamp() <= written3.getTimestamp() );

        //now run the cleanup

        final List<ConnectionScope> deletedConnections =
            connectionService.deDupeConnections( Observable.just( applicationScope ) ).toList().toBlocking().last();

        //check our oldest was deleted first
        assertEdgeData( written2, deletedConnections.get( 0 ).getEdge() );

        assertEdgeData( written1, deletedConnections.get( 1 ).getEdge() );

        assertEquals( "2 edges deleted", 2, deletedConnections.size() );



        //now ensure we can read the edge.

        final SearchByEdge simpleSearchByEdge =
            new SimpleSearchByEdge( source, connection1.getType(), target, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.absent() );

        //check only 1 exists
        final List<Edge> edges = gm.loadEdgeVersions( simpleSearchByEdge ).toList().toBlocking().last();

        assertEquals( 1, edges.size() );

        assertEquals( written3, edges.get( 0 ) );
    }


    /**
     * Compares edges based on their sourceId, type, targetId and timestamp. It ignores the deleted flag
     * @param expected
     * @param asserted
     */
    private void assertEdgeData(final Edge expected, final Edge asserted){
        assertEquals("SourceId the same", expected.getSourceNode(), asserted.getSourceNode());

        assertEquals("TargetId the same", expected.getTargetNode(), asserted.getTargetNode());

        assertEquals("Type the same", expected.getType(), asserted.getType());

        assertEquals("Timestamp the same", expected.getTimestamp(), asserted.getTimestamp());

    }
}
