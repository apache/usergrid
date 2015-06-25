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
import java.util.Iterator;
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
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.guice.CurrentImpl;
import org.apache.usergrid.persistence.core.guice.PreviousImpl;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManagerImpl;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Injector;
import com.google.inject.Key;

import net.jcip.annotations.NotThreadSafe;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
public class EntityDataMigrationIT extends AbstractCoreIT {


    private Injector injector;


    private EntityDataMigration entityDataMigration;
    private ManagerCache managerCache;
    private DataMigrationManager dataMigrationManager;
    private MigrationInfoSerialization migrationInfoSerialization;
    private MvccEntitySerializationStrategy v1Strategy;
    private MvccEntitySerializationStrategy v2Strategy;
    private EntityManagerFactory emf;



    /**
     * Rule to do the resets we need
     */
    @Rule
    public MigrationTestRule migrationTestRule =
            new MigrationTestRule( app,  SpringResource.getInstance().getBean( Injector.class ) ,EntityDataMigration.class  );

    @Before
    public void setup() {
        emf = setup.getEmf();
        injector =  SpringResource.getInstance().getBean( Injector.class );
        entityDataMigration = injector.getInstance( EntityDataMigration.class );
        managerCache = injector.getInstance( ManagerCache.class );
        dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        migrationInfoSerialization = injector.getInstance( MigrationInfoSerialization.class );
        v1Strategy = injector.getInstance( Key.get(MvccEntitySerializationStrategy.class, PreviousImpl.class) );
        v2Strategy = injector.getInstance( Key.get(MvccEntitySerializationStrategy.class, CurrentImpl.class) );
    }


    @Test
    @Ignore("Awaiting fix for USERGRID-268")
    public void testDataMigration() throws Throwable {

        assertEquals( "version 3 expected", 3, entityDataMigration.getVersion() );
        assertEquals( "Previous version expected", 2, dataMigrationManager.getCurrentVersion());


        final EntityManager newAppEm = app.getEntityManager();

        final String type1 = "type1thing";
        final String type2 = "type2thing";
        final int size = 10;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( newAppEm, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( newAppEm, type2, size );


        final Set<Id> createdEntityIds = new HashSet<>();
        createdEntityIds.addAll( type1Identities );
        createdEntityIds.addAll( type2Identities );


        final TestProgressObserver progressObserver = new TestProgressObserver();


        //load everything that appears in v1, migrate and ensure it appears in v2
        final Set<MvccEntity> savedEntities = new HashSet<>( 10000 );
        //set that holds all entityIds for later assertion
        final Set<Id> entityIds = new HashSet<>(10000);


        //read everything in previous version format and put it into our types.  Assumes we're
        //using a test system, and it's not a huge amount of data, otherwise we'll overflow.

        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 )
            .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {
                @Override
                public void call(
                        final AllEntitiesInSystemObservable.ApplicationEntityGroup entity ) {

                    //add all versions from history to our comparison
                    for ( final Id id : entity.entityIds ) {

                        CollectionScope scope = CpNamingUtils
                                .getCollectionScopeNameFromEntityType(
                                        entity.applicationScope.getApplication(),
                                        id.getType() );

                        final Iterator<MvccEntity> versions = v1Strategy
                                .loadDescendingHistory( scope, id, UUIDGenerator.newTimeUUID(),
                                        100 );

                        while ( versions.hasNext() ) {

                            final MvccEntity mvccEntity = versions.next();

                            savedEntities.add( mvccEntity );

                            createdEntityIds.remove( mvccEntity.getId() );

                            entityIds.add( id );
                        }
                    }
                }
            } ).toBlocking().lastOrDefault( null );

        assertEquals( "Newly saved entities encountered", 0, createdEntityIds.size() );
        assertTrue( "Saved new entities", savedEntities.size() > 0 );

        //perform the migration
        entityDataMigration.migrate( progressObserver );

        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //write the status and version, then invalidate the cache so it appears
        migrationInfoSerialization.setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );
        migrationInfoSerialization.setVersion( entityDataMigration.getVersion() );
        dataMigrationManager.invalidate();

        assertEquals( "New version saved, and we should get new implementation", entityDataMigration.getVersion(),
                dataMigrationManager.getCurrentVersion() );


        //now visit all entities in the system again, load them from v2, and ensure they're the same
        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 )
            .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {
                @Override
                public void call(
                        final AllEntitiesInSystemObservable
                                .ApplicationEntityGroup entity ) {
                    //add all versions from history to our comparison
                    for ( final Id id : entity.entityIds ) {

                        CollectionScope scope = CpNamingUtils
                                .getCollectionScopeNameFromEntityType(
                                        entity.applicationScope.getApplication(),
                                        id.getType() );

                        final Iterator<MvccEntity> versions = v2Strategy
                                .loadDescendingHistory( scope, id,
                                        UUIDGenerator.newTimeUUID(), 100 );

                        while ( versions.hasNext() ) {

                            final MvccEntity mvccEntity = versions.next();

                            savedEntities.remove( mvccEntity );
                        }
                    }
                }
            }


            ).toBlocking().lastOrDefault( null );


        assertEquals( "All entities migrated", 0, savedEntities.size() );


        //now visit all entities in the system again, and load them from the EM,
        // ensure we see everything we did in the v1 traversal
        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache, 1000 )
            .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {
                @Override
                public void call(
                        final AllEntitiesInSystemObservable
                                .ApplicationEntityGroup entity ) {

                    final EntityManager em = emf.getEntityManager(
                            entity.applicationScope.getApplication().getUuid() );

                    //add all versions from history to our comparison
                    for ( final Id id : entity.entityIds ) {


                        try {
                            final Entity emEntity = em.get( SimpleEntityRef.fromId( id ) );

                            if(emEntity != null){
                                entityIds.remove( id );
                            }
                        }
                        catch ( Exception e ) {
                            throw new RuntimeException("Error loading entity", e);
                        }
                    }
                }
            }


            ).toBlocking().lastOrDefault( null );


        assertEquals("All entities could be loaded by the entity manager", 0, entityIds.size());


    }
}
