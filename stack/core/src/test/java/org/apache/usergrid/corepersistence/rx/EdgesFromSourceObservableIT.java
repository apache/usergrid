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


import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Injector;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests that when we create a few entities, we get their data.
 */
public class EdgesFromSourceObservableIT extends AbstractCoreIT {


    @Test
    public void testEntities() throws Exception {

        final EntityManager em = app.getEntityManager();
        final Application createdApplication = em.getApplication();



        final String type1 = "targetthings";
        final String type2 = "sourcethings";
        final int size = 10;



        final Set<Id> sourceIdentities = EntityWriteHelper.createTypes( em, type2, size );


        final Entity entity = em.create( type1, new HashMap<String, Object>(){{put("property", "value");}} );
                  final Id target = new SimpleId( entity.getUuid(), entity.getType() );




        for ( Id source : sourceIdentities ) {
            em.createConnection( SimpleEntityRef.fromId( source ), "likes", SimpleEntityRef.fromId( target ) );
        }


        //this is hacky, but our context integration b/t guice and spring is a mess.  We need to clean this up when we
        //clean up our wiring
        //
        ManagerCache managerCache =  SpringResource.getInstance().getBean( Injector.class ).getInstance( ManagerCache.class );


        final ApplicationScope scope = CpNamingUtils.getApplicationScope( app.getId() );


        final GraphManager gm = managerCache.getGraphManager( scope );

        EdgesToTargetObservable.getEdgesToTarget( gm, target ).doOnNext( new Action1<Edge>() {
            @Override
            public void call( final Edge edge ) {
                final String edgeType = edge.getType();
                final Id source = edge.getSourceNode();

                //test if we're a collection, if so
                if ( CpNamingUtils.isCollectionEdgeType( edgeType ) ) {
                    final String collectionName = CpNamingUtils.getCollectionName( edgeType );

                    assertEquals("application source returned", createdApplication.getUuid(), source.getUuid());

                    final String expectedCollection = Schema.defaultCollectionName( target.getType() );

                    assertEquals("right type returned", expectedCollection, collectionName);

                    return;
                }



                if ( !CpNamingUtils.isConnectionEdgeType( edgeType ) ) {
                    fail( "Only connection edges should be encountered" );
                }

                final String connectionType = CpNamingUtils.getConnectionType( edgeType );

                assertEquals( "Same connection type expected", "likes", connectionType );


                assertTrue( "Element should be present on removal", sourceIdentities.remove( source ) );



            }
        } ).toBlocking().lastOrDefault( null );


    }


}
