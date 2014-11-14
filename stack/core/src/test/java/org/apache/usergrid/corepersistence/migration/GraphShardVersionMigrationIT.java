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
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManagerImpl;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerializationImpl;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import com.netflix.astyanax.Keyspace;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GraphShardVersionMigrationIT extends AbstractCoreIT {

    private Injector injector;
    private GraphShardVersionMigration graphShardVersionMigration;
    private Keyspace keyspace;
    private MigrationManager migrationManager;
    private EntityManagerFactory emf;
    private ManagerCache managerCache;
    private DataMigrationManager dataMigrationManager;
    private MigrationInfoSerialization migrationInfoSerialization;


    @Before
    public void setup() {
        injector = CpSetup.getInjector();
        emf = setup.getEmf();
        graphShardVersionMigration = injector.getInstance( GraphShardVersionMigration.class );
        keyspace = injector.getInstance( Keyspace.class );
        migrationManager = injector.getInstance( MigrationManager.class );
        managerCache = injector.getInstance( ManagerCache.class );
        dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        migrationInfoSerialization = injector.getInstance( MigrationInfoSerialization.class );
    }


    @Test
    public void testIdMapping() throws Throwable {


        /**
         * Drop our migration keyspaces to ensure we don't have a "new version in there"
         * This will ensure we have an "old data" version of data written
         */
        keyspace.dropColumnFamily( MigrationInfoSerializationImpl.CF_MIGRATION_INFO );

        //create the column families again
        migrationManager.migrate();

        final EntityManager newAppEm = app.getEntityManager();

        final String type1 = "type1thing";
        final String type2 = "type2thing";
        final int size = 10;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( newAppEm, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( newAppEm, type2, size );


        final Set<Id> allEntities = new HashSet<>();
        allEntities.addAll( type1Identities );
        allEntities.addAll( type2Identities );


        final TestProgressObserver progressObserver = new TestProgressObserver();


        //used to validate 1.0 types, and 2.0 types
        final Multimap<Id, String> sourceTypes = HashMultimap.create( 10000, 10 );
        final Multimap<Id, String> targetTypes = HashMultimap.create( 10000, 10 );


        //read everything in previous version format and put it into our types.

        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache )
                                     .doOnNext( new Action1<AllEntitiesInSystemObservable.EntityData>() {
                                         @Override
                                         public void call( final AllEntitiesInSystemObservable.EntityData entity ) {

                                             final GraphManager gm =
                                                     managerCache.getGraphManager( entity.applicationScope );

                                             /**
                                              * Get our edge types from the source
                                              */
                                             gm.getEdgeTypesFromSource(
                                                     new SimpleSearchEdgeType( entity.entityId, null, null ) )
                                               .doOnNext( new Action1<String>() {
                                                   @Override
                                                   public void call( final String s ) {
                                                       sourceTypes.put( entity.entityId, s );
                                                   }
                                               } ).toBlocking().lastOrDefault( null );


                                             /**
                                              * Get the edge types to the target
                                              */
                                             gm.getEdgeTypesToTarget(
                                                     new SimpleSearchEdgeType( entity.entityId, null, null ) )
                                               .doOnNext( new Action1<String>() {
                                                   @Override
                                                   public void call( final String s ) {
                                                       targetTypes.put( entity.entityId, s );
                                                   }
                                               } ).toBlocking().lastOrDefault( null );

                                             allEntities.remove( entity.entityId );
                                         }
                                     } ).toBlocking().lastOrDefault( null );


        //perform the migration
        graphShardVersionMigration.migrate( progressObserver );

        assertEquals("Newly saved entities encounterd", 0, allEntities.size());
        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //write the status and version, then invalidate the cache so it appears
        migrationInfoSerialization.setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );
        migrationInfoSerialization.setVersion( graphShardVersionMigration.getVersion() );
        dataMigrationManager.invalidate();

        assertEquals("New version saved, and we should get new implementation", graphShardVersionMigration.getVersion(), dataMigrationManager.getCurrentVersion());


        //now visit all nodes in the system and remove their types from the multi maps, it should be empty at the end
        AllEntitiesInSystemObservable.getAllEntitiesInSystem( managerCache )
                                     .doOnNext( new Action1<AllEntitiesInSystemObservable.EntityData>() {
                                         @Override
                                         public void call( final AllEntitiesInSystemObservable.EntityData entity ) {

                                             final GraphManager gm =
                                                     managerCache.getGraphManager( entity.applicationScope );

                                             /**
                                              * Get our edge types from the source
                                              */
                                             gm.getEdgeTypesFromSource(
                                                     new SimpleSearchEdgeType( entity.entityId, null, null ) )
                                               .doOnNext( new Action1<String>() {
                                                   @Override
                                                   public void call( final String s ) {
                                                       sourceTypes.remove( entity.entityId, s );
                                                   }
                                               } ).toBlocking().lastOrDefault( null );


                                             /**
                                              * Get the edge types to the target
                                              */
                                             gm.getEdgeTypesToTarget(
                                                     new SimpleSearchEdgeType( entity.entityId, null, null ) )
                                               .doOnNext( new Action1<String>() {
                                                   @Override
                                                   public void call( final String s ) {
                                                       targetTypes.remove( entity.entityId, s );
                                                   }
                                               } ).toBlocking().lastOrDefault( null );
                                         }
                                     } ).toBlocking().lastOrDefault( null );

        assertEquals( "All source types migrated", 0, sourceTypes.size() );
        assertEquals( "All target types migrated", 0, targetTypes.size() );
    }
}
