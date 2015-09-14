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


@RunWith( ITRunner.class )
@UseModules( TestCoreModule.class )
public class ConnectionServiceImplTest {

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

        final Id target = new SimpleId( "target" );

        final String connectionType = "testConnection";


          //add to a collection

        final String collectionName = "testCollection";

        final Edge collectionEdge = CpNamingUtils.createCollectionEdge( applicationScope.getApplication(), collectionName, source );

        final Edge writtenCollection = gm.writeEdge( collectionEdge ).toBlocking().last();

        assertNotNull("Collection edge written", writtenCollection);

        //write our first connection
        final Edge connection1 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written1 = gm.writeEdge( connection1 ).toBlocking().last();


        //write the second
        final Edge connection2 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written2 = gm.writeEdge( connection2 ).toBlocking().last();


        //write the 3rd
        final Edge connection3 = CpNamingUtils.createConnectionEdge( source, connectionType, target );

        final Edge written3 = gm.writeEdge( connection3 ).toBlocking().last();




        //now run the cleanup

        final List<ConnectionScope> deletedConnections =
            connectionService.deDupeConnections( Observable.just( applicationScope ) ).toList().toBlocking().last();

        assertEquals( "2 edges deleted", 2, deletedConnections.size() );

        //check our oldest was deleted first

        assertEquals(written2, deletedConnections.get( 0 ));

        assertEquals(written3, deletedConnections.get( 1 ));



        //now ensure we can read the edge.

        final SearchByEdge simpleSearchByEdge =
            new SimpleSearchByEdge( source, connection1.getType(), target, Long.MAX_VALUE,
                SearchByEdgeType.Order.DESCENDING, Optional.absent() );

        //check only 1 exists
        final List<Edge> edges = gm.loadEdgeVersions( simpleSearchByEdge ).toList().toBlocking().last();

        assertEquals( 1, edges.size() );

        assertEquals( written1, edges.get( 0 ) );
    }
}
