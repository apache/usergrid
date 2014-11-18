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

package org.apache.usergrid.corepersistence.migration;


import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.map.impl.MapSerializationImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Injector;
import com.netflix.astyanax.Keyspace;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class EntityTypeMappingMigrationIT extends AbstractCoreIT {


    private Injector injector;


    private EntityTypeMappingMigration entityTypeMappingMigration;
    private Keyspace keyspace;
    private EntityManagerFactory emf;
    private ManagerCache managerCache;


    @Before
    public void setup() {
        injector = CpSetup.getInjector();
        emf = setup.getEmf();
        entityTypeMappingMigration = injector.getInstance( EntityTypeMappingMigration.class );
        keyspace = injector.getInstance( Keyspace.class );
        managerCache = injector.getInstance( ManagerCache.class );
    }


    @Test
    public void testIdMapping() throws Throwable {

        assertEquals("version 1 expected", 1, entityTypeMappingMigration.getVersion());

        final EntityManager newAppEm = app.getEntityManager();

        final String type1 = "type1thing";
        final String type2 = "type2thing";
        final int size = 10;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( newAppEm, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( newAppEm, type2, size );



        final Set<Id> allEntities = new HashSet<>();
        allEntities.addAll( type1Identities );
        allEntities.addAll( type2Identities );


        /**
         * Drop our map keyspace to ensure we have no entries before migrating after doing our writes.
         * This will ensure we have the data
         */
        keyspace.truncateColumnFamily( MapSerializationImpl.MAP_ENTRIES );
        keyspace.truncateColumnFamily( MapSerializationImpl.MAP_KEYS );


        final TestProgressObserver progressObserver = new TestProgressObserver();

        entityTypeMappingMigration.migrate( progressObserver );





        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache )
                                     .doOnNext( new Action1<AllEntitiesInSystemObservable.EntityData>() {
                                         @Override
                                         public void call( final AllEntitiesInSystemObservable.EntityData entity ) {
                                             //ensure that each one has a type
                                             try {

                                                 final EntityManager em = emf.getEntityManager( entity.applicationScope.getApplication().getUuid() );
                                                 final Entity returned = em.get( entity.entityId.getUuid() );

                                                 //we seem to occasionally get phantom edges.  If this is the case we'll store the type _> uuid mapping, but we won't have anything to load
                                                if(returned != null) {
                                                    assertEquals( entity.entityId.getUuid(), returned.getUuid() );
                                                    assertEquals( entity.entityId.getType(), returned.getType() );
                                                }
                                                else {
                                                    final String type = managerCache.getMapManager( CpNamingUtils.getEntityTypeMapScope(
                                                            entity.applicationScope.getApplication() ) )
                                                            .getString( entity.entityId.getUuid().toString() );

                                                    assertEquals(entity.entityId.getType(), type);
                                                }
                                             }
                                             catch ( Exception e ) {
                                                 throw new RuntimeException( "Unable to get entity " + entity.entityId
                                                         + " by UUID, migration failed", e );
                                             }

                                             allEntities.remove( entity.entityId );
                                         }
                                     } ).toBlocking().lastOrDefault( null );


        assertEquals( "Every element should have been encountered", 0, allEntities.size() );
        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


    }


}
