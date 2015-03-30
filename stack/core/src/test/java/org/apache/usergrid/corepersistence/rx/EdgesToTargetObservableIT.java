/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.rx;


import java.util.HashSet;
import java.util.Set;

import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.utils.EdgeTestUtils;

import com.google.inject.Injector;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests that when we create a few entities, we get their data.
 */
public class EdgesToTargetObservableIT extends AbstractCoreIT {


    @Test
    public void testEntities() throws Exception {

        Injector injector = SpringResource.getInstance().getBean( Injector.class );
        EdgesObservable edgesFromSourceObservable=  injector.getInstance(EdgesObservable.class);
        final EntityManager em = app.getEntityManager();

        final String type1 = "type1things";
        final String type2 = "type2things";
        final int size = 10;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( em, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( em, type2, size );

        //create a connection and put that in our connection types
        final Id source = type1Identities.iterator().next();


        final Set<Id> connections = new HashSet<>();

        for ( Id target : type2Identities ) {
            em.createConnection( SimpleEntityRef.fromId( source ), "likes", SimpleEntityRef.fromId( target ) );
            connections.add( target );
        }


        //this is hacky, but our context integration b/t guice and spring is a mess.  We need to clean this up when we
        //clean up our wiring
        //
        ManagerCache managerCache =  SpringResource.getInstance().getBean( Injector.class ).getInstance( ManagerCache.class );


        final ApplicationScope scope = CpNamingUtils.getApplicationScope( app.getId() );
        final Id applicationId = scope.getApplication();


        final GraphManager gm = managerCache.getGraphManager( scope );

        edgesFromSourceObservable.edgesFromSource( gm, applicationId ).doOnNext( edge -> {
            final String edgeType = edge.getType();
            final Id target = edge.getTargetNode();

            //test if we're a collection, if so remove ourselves fro the types
            if ( !EdgeTestUtils.isCollectionEdgeType( edgeType ) ) {
                fail( "Connections should be the only type encountered" );
            }


            final String collectionType = EdgeTestUtils.getNameForEdge( edgeType );

            if ( collectionType.equals( type1 ) ) {
                assertTrue( "Element should be present on removal", type1Identities.remove( target ) );
            }
            else if ( collectionType.equals( type2 ) ) {
                assertTrue( "Element should be present on removal", type2Identities.remove( target ) );
            }


        } ).toBlocking().lastOrDefault( null );


        assertEquals( "Every element should have been encountered", 0, type1Identities.size() );
        assertEquals( "Every element should have been encountered", 0, type2Identities.size() );


        //test connections

        edgesFromSourceObservable.edgesFromSource( gm, source).doOnNext( edge -> {
            final String edgeType = edge.getType();
            final Id target = edge.getTargetNode();

            if ( !EdgeTestUtils.isConnectionEdgeType( edgeType ) ) {
                fail( "Only connection edges should be encountered" );
            }

            final String connectionType = EdgeTestUtils.getNameForEdge( edgeType );

            assertEquals( "Same connection type expected", "likes", connectionType );


            assertTrue( "Element should be present on removal", connections.remove( target ) );
        } ).toBlocking().lastOrDefault( null );

        assertEquals( "Every connection should have been encountered", 0, connections.size() );
    }


}
