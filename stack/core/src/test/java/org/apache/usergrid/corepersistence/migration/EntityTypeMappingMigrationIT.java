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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.map.impl.MapSerializationImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Injector;
import com.netflix.astyanax.Keyspace;

import net.jcip.annotations.NotThreadSafe;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
public class EntityTypeMappingMigrationIT extends AbstractCoreIT {


    private Injector injector;


    private EntityTypeMappingMigration entityTypeMappingMigration;
    private Keyspace keyspace;
    private EntityManagerFactory emf;
    private ManagerCache managerCache;
    private DataMigrationManager dataMigrationManager;


    /**
     * Rule to do the resets we need
     */
    @Rule
    public MigrationTestRule migrationTestRule = new MigrationTestRule(
            app,  SpringResource.getInstance().getBean( Injector.class ) ,EntityTypeMappingMigration.class  );



    @Before
    public void setup() {
        injector =  SpringResource.getInstance().getBean( Injector.class );
        emf = setup.getEmf();
        entityTypeMappingMigration = injector.getInstance( EntityTypeMappingMigration.class );
        keyspace = injector.getInstance( Keyspace.class );
        managerCache = injector.getInstance( ManagerCache.class );
        dataMigrationManager = injector.getInstance( DataMigrationManager.class );
    }


    @Test
    @Ignore("Ignored awaiting fix for USERGRID-268")
    public void testIdMapping() throws Throwable {

        assertEquals( "version 1 expected", 1, entityTypeMappingMigration.getVersion() );
        assertEquals( "Previous version expected", 0, dataMigrationManager.getCurrentVersion());

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

        app.createApplication(
                GraphShardVersionMigrationIT.class.getSimpleName()+ UUIDGenerator.newTimeUUID(),
                "migrationTest" );



        final TestProgressObserver progressObserver = new TestProgressObserver();

        entityTypeMappingMigration.migrate( progressObserver );


        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 )
            .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {
                @Override
                public void call(
                        final AllEntitiesInSystemObservable.ApplicationEntityGroup entity ) {
                    //ensure that each one has a type

                    final EntityManager em = emf.getEntityManager(
                            entity.applicationScope.getApplication().getUuid() );

                    for ( final Id id : entity.entityIds ) {
                        try {
                            final Entity returned = em.get( id.getUuid() );

                            //we seem to occasionally get phantom edges.  If this is the
                            // case we'll store the type _> uuid mapping, but we won't have
                            // anything to load

                            if ( returned != null ) {
                                assertEquals( id.getUuid(), returned.getUuid() );
                                assertEquals( id.getType(), returned.getType() );
                            }
                            else {
                                final String type = managerCache.getMapManager( CpNamingUtils
                                        .getEntityTypeMapScope(
                                                entity.applicationScope.getApplication() ) )
                                                                .getString( id.getUuid()
                                                                            .toString() );

                                assertEquals( id.getType(), type );
                            }
                        }
                        catch ( Exception e ) {
                            throw new RuntimeException( "Unable to get entity " + id
                                    + " by UUID, migration failed", e );
                        }

                        allEntities.remove( id );
                    }
                }
            } ).toBlocking().lastOrDefault( null );


        assertEquals( "Every element should have been encountered", 0, allEntities.size() );
        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );
    }
}
