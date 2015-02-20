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

import org.apache.usergrid.corepersistence.CpSetup;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.core.migration.data.*;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.rx.ApplicationObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeDataMigrationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationProxyImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.impl.AllEntitiesInSystemObservableImpl;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;

import net.jcip.annotations.NotThreadSafe;

import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
public class GraphShardVersionMigrationIT extends AbstractCoreIT {

    private Injector injector;
    private ApplicationDataMigration graphShardVersionMigration;
    private ManagerCache managerCache;
    private DataMigrationManager dataMigrationManager;
    private MigrationInfoSerialization migrationInfoSerialization;


    /**
     * Rule to do the resets we need
     */
    @Rule
    public MigrationTestRule migrationTestRule = new MigrationTestRule( app,  SpringResource.getInstance().getBean(Injector.class) ,EdgeDataMigrationImpl.class  );
    private AllEntitiesInSystemObservable allEntitiesInSystemObservable;
    private ApplicationObservable applicationObservable;


    @Before
    public void setup() {

        injector =  SpringResource.getInstance().getBean( Injector.class );
        graphShardVersionMigration = injector.getInstance( EdgeDataMigrationImpl.class );
        managerCache = injector.getInstance( ManagerCache.class );
        dataMigrationManager = injector.getInstance( DataMigrationManager.class );
        migrationInfoSerialization = injector.getInstance( MigrationInfoSerialization.class );
        allEntitiesInSystemObservable = injector.getInstance(AllEntitiesInSystemObservable.class);
        applicationObservable = injector.getInstance(ApplicationObservable.class);

    }


    @Test
    @Ignore("Ignored awaiting fix for USERGRID-268")
    public void testIdMapping() throws Throwable {

        assertEquals( "version 2 expected", 2, graphShardVersionMigration.getVersion() );
        assertEquals( "Previous version expected", 1, dataMigrationManager.getCurrentVersion());



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

        allEntitiesInSystemObservable.getAllEntitiesInSystem( 1000)
                                     .doOnNext( new Action1<ApplicationEntityGroup<CollectionScope>>() {
                                         @Override
                                         public void call(
                                                 final ApplicationEntityGroup<CollectionScope> entity ) {

                                             final GraphManager gm =
                                                     managerCache.getGraphManager( entity.applicationScope );

                                             for ( final EntityIdScope<CollectionScope> idScope : entity.entityIds ) {
                                                 /**
                                                  * Get our edge types from the source
                                                  */
                                                 gm.getEdgeTypesFromSource( new SimpleSearchEdgeType(idScope.getId(), null, null ) )
                                                   .doOnNext(new Action1<String>() {
                                                       @Override
                                                       public void call(final String s) {
                                                           sourceTypes.put(idScope.getId(), s);
                                                       }
                                                   }).toBlocking().lastOrDefault( null );


                                                 /**
                                                  * Get the edge types to the target
                                                  */
                                                 gm.getEdgeTypesToTarget( new SimpleSearchEdgeType( idScope.getId(), null, null ) )
                                                   .doOnNext( new Action1<String>() {
                                                       @Override
                                                       public void call( final String s ) {
                                                           targetTypes.put( idScope.getId(), s );
                                                       }
                                                   } ).toBlocking().lastOrDefault( null );

                                                 allEntities.remove( idScope.getId() );
                                             }
                                         }
                                     } ).toBlocking().lastOrDefault( null );


        //perform the migration

        graphShardVersionMigration.migrate(applicationObservable.getAllApplicationScopes(), progressObserver).toBlocking().last();

        assertEquals( "Newly saved entities encounterd", 0, allEntities.size() );
        assertFalse( "Progress observer should not have failed", progressObserver.getFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //write the status and version, then invalidate the cache so it appears
        migrationInfoSerialization.setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );
        migrationInfoSerialization.setVersion( graphShardVersionMigration.getVersion() );
        dataMigrationManager.invalidate();

        assertEquals( "New version saved, and we should get new implementation",
                graphShardVersionMigration.getVersion(), dataMigrationManager.getCurrentVersion() );


        //now visit all nodes in the system and remove their types from the multi maps, it should be empty at the end
        allEntitiesInSystemObservable.getAllEntitiesInSystem( 1000)
                                     .doOnNext( new Action1<ApplicationEntityGroup<CollectionScope>>() {
                                                    @Override
                                                    public void call(
                                                            final ApplicationEntityGroup<CollectionScope> entity ) {

                                                        final GraphManager gm =
                                                                managerCache.getGraphManager( entity.applicationScope );

                                                        for ( final EntityIdScope<CollectionScope> idScope : entity.entityIds ) {
                                                            /**
                                                             * Get our edge types from the source
                                                             */
                                                            gm.getEdgeTypesFromSource(
                                                                    new SimpleSearchEdgeType( idScope.getId(), null, null ) )
                                                              .doOnNext( new Action1<String>() {
                                                                  @Override
                                                                  public void call( final String s ) {
                                                                      sourceTypes.remove( idScope.getId(), s );
                                                                  }
                                                              } ).toBlocking().lastOrDefault( null );


                                                            /**
                                                             * Get the edge types to the target
                                                             */
                                                            gm.getEdgeTypesToTarget(
                                                                    new SimpleSearchEdgeType( idScope.getId(), null, null ) )
                                                              .doOnNext( new Action1<String>() {
                                                                  @Override
                                                                  public void call( final String s ) {
                                                                      targetTypes.remove( idScope.getId(), s );
                                                                  }
                                                              } ).toBlocking().lastOrDefault( null );
                                                        }
                                                    }
                                                }


                                              ).toBlocking().lastOrDefault( null );


        assertEquals( "All source types migrated", 0, sourceTypes.size() );


        assertEquals( "All target types migrated", 0, targetTypes.size() );
    }
}
