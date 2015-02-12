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

import org.apache.usergrid.persistence.collection.mvcc.MvccEntityMigrationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntityDataMigrationImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyProxyV2Impl;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
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


    private DataMigration entityDataMigration;
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

            new MigrationTestRule( app, SpringResource.getInstance().getBean( Injector.class ) ,MvccEntityDataMigrationImpl.class  );
    private AllEntitiesInSystemObservable allEntitiesInSystemObservable;


    @Before
    public void setup() {
        emf = setup.getEmf();
        injector = SpringResource.getInstance().getBean(Injector.class);
        entityDataMigration = injector.getInstance( MvccEntityDataMigrationImpl.class );
        injector =  SpringResource.getInstance().getBean( Injector.class );
        dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        migrationInfoSerialization = injector.getInstance( MigrationInfoSerialization.class );
        MvccEntityMigrationStrategy strategy = injector.getInstance(Key.get(MvccEntityMigrationStrategy.class));
        allEntitiesInSystemObservable = injector.getInstance(AllEntitiesInSystemObservable.class);
        v1Strategy = strategy.getMigration().from();
        v2Strategy = strategy.getMigration().to();
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

        allEntitiesInSystemObservable.getAllEntitiesInSystem(  1000)
            .doOnNext( new Action1<ApplicationEntityGroup>() {
                @Override
                public void call(
                        final ApplicationEntityGroup entity ) {

                    //add all versions from history to our comparison
                    for ( final EntityIdScope id : entity.entityIds ) {

                        CollectionScope scope = CpNamingUtils
                                .getCollectionScopeNameFromEntityType(
                                    entity.applicationScope.getApplication(),
                                    id.getId().getType());

                        final Iterator<MvccEntity> versions = v1Strategy
                                .loadDescendingHistory( scope, id.getId(), UUIDGenerator.newTimeUUID(),
                                        100 );

                        while ( versions.hasNext() ) {

                            final MvccEntity mvccEntity = versions.next();

                            savedEntities.add( mvccEntity );

                            createdEntityIds.remove( mvccEntity.getId() );

                            entityIds.add( id.getId() );
                        }
                    }
                }
            } ).toBlocking().lastOrDefault( null );

        assertEquals( "Newly saved entities encountered", 0, createdEntityIds.size() );
        assertTrue( "Saved new entities", savedEntities.size() > 0 );

        //perform the migration
        allEntitiesInSystemObservable.getAllEntitiesInSystem(  1000)
            .doOnNext(new Action1<ApplicationEntityGroup>() {
                @Override
                public void call(ApplicationEntityGroup applicationEntityGroup) {
                   try {
                       entityDataMigration.migrate(applicationEntityGroup, progressObserver).toBlocking().last();
                   }catch (Throwable e){
                       throw new RuntimeException(e);
                   }
                }
            }).toBlocking().last();


        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //write the status and version, then invalidate the cache so it appears
        migrationInfoSerialization.setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );
        migrationInfoSerialization.setVersion( entityDataMigration.getVersion() );
        dataMigrationManager.invalidate();

        assertEquals( "New version saved, and we should get new implementation", entityDataMigration.getVersion(),
                dataMigrationManager.getCurrentVersion() );


        //now visit all entities in the system again, load them from v2, and ensure they're the same
        allEntitiesInSystemObservable.getAllEntitiesInSystem( 1000)
            .doOnNext( new Action1<ApplicationEntityGroup>() {
                @Override
                public void call(
                        final ApplicationEntityGroup entity ) {
                    //add all versions from history to our comparison
                    for ( final EntityIdScope id : entity.entityIds ) {

                        CollectionScope scope = CpNamingUtils
                                .getCollectionScopeNameFromEntityType(
                                    entity.applicationScope.getApplication(),
                                    id.getId().getType());

                        final Iterator<MvccEntity> versions = v2Strategy
                                .loadDescendingHistory( scope, id.getId(),
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
        allEntitiesInSystemObservable.getAllEntitiesInSystem( 1000)
            .doOnNext( new Action1<ApplicationEntityGroup>() {
                @Override
                public void call(
                        final ApplicationEntityGroup entity ) {

                    final EntityManager em = emf.getEntityManager(
                            entity.applicationScope.getApplication().getUuid() );

                    //add all versions from history to our comparison
                    for ( final EntityIdScope id : entity.entityIds ) {


                        try {
                            final Entity emEntity = em.get( SimpleEntityRef.fromId( id.getId() ) );

                            if(emEntity != null){
                                entityIds.remove( id.getId() );
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
