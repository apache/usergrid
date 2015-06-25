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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Injector;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests that when we create a few entities, we get their data.
 */
public class AllEntitiesInSystemObservableIT extends AbstractCoreIT {

    private final Logger logger = LoggerFactory.getLogger( AllEntitiesInSystemObservableIT.class );

    @Test
    public void testEntities() throws Exception {

        final EntityManager em = app.getEntityManager();

        final String type1 = "type1thing";
        final String type2 = "type2thing";
        final int size = 10;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( em, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( em, type2, size );

        //create a connection and put that in our connection types
        final Id source = type1Identities.iterator().next();


        final Set<Id> allEntities = new HashSet<>();
        allEntities.addAll( type1Identities );
        allEntities.addAll( type2Identities );

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

        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 ).doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {
            @Override
            public void call( final AllEntitiesInSystemObservable.ApplicationEntityGroup entity ) {

                assertNotNull(entity);
                assertNotNull(entity.applicationScope);
                assertNotNull(entity.entityIds);

                //not from our test, don't check it
                if(!applicationId.equals( entity.applicationScope.getApplication() )){
                    return;
                }

                for(Id id: entity.entityIds) {

                    //we should only emit each node once
                    if ( id.getType().equals( type1 ) ) {
                        assertTrue( "Element should be present on removal", type1Identities.remove( id ) );
                    }
                    else if ( id.getType().equals( type2 ) ) {
                        assertTrue( "Element should be present on removal", type2Identities.remove( id ) );
                    }
                }
            }
        } ).toBlocking().lastOrDefault( null );


        assertEquals( "Every element should have been encountered", 0, type1Identities.size() );
        assertEquals( "Every element should have been encountered", 0, type2Identities.size() );


        //test connections

        TargetIdObservable.getTargetNodes( gm, source ).doOnNext( new Action1<Id>() {
            @Override
            public void call( final Id target ) {

                assertTrue( "Element should be present on removal", connections.remove( target ) );
            }
        } ).toBlocking().lastOrDefault( null );

        assertEquals( "Every connection should have been encountered", 0, connections.size() );
    }

}
