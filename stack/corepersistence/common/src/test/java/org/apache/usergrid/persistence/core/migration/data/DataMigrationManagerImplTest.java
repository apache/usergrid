/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * Tests our data migration manager
 */
public class DataMigrationManagerImplTest {

    //TODO USERGRID-405 fix this
//
//    AllEntitiesInSystemObservable allEntitiesInSystemObservable = new AllEntitiesInSystemObservable() {
//        @Override
//        public Observable<ApplicationEntityGroup> getAllEntitiesInSystem(int bufferSize) {
//
//            return Observable.create(new Observable.OnSubscribe<ApplicationEntityGroup>() {
//                @Override
//                public void call(Subscriber<? super ApplicationEntityGroup> subscriber) {
//                    ApplicationEntityGroup entityGroup = new ApplicationEntityGroup(mock(ApplicationScope.class),new ArrayList<EntityIdScope>());
//                    subscriber.onNext(entityGroup);
//                    subscriber.onCompleted();
//                }
//            });
//        }
//
//        @Override
//        public Observable<ApplicationEntityGroup> getAllEntitiesInSystem(Observable appIdObservable, int bufferSize) {
//            return this.getAllEntitiesInSystem(bufferSize) ;
//        }
//    };
//
//    ApplicationObservable allApplicationsObservable = new ApplicationObservable() {
//        @Override
//        public Observable<Id> getAllApplicationIds() {
//            return Observable.just( (Id)new SimpleId("application"));
//        }
//
//        @Override
//        public Observable<ApplicationScope> getAllApplicationScopes() {
//            return Observable.just( (ApplicationScope)new ApplicationScopeImpl((Id)new SimpleId("application")));
//        }
//    };
//
//    @Test
//    public void noMigrations() throws MigrationException {
//        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );
//        when(serialization.getCurrentVersion()).thenReturn(1);
//
//        Set<DataMigration> emptyMigration = new HashSet<>();
//
//        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, emptyMigration, allEntitiesInSystemObservable,allApplicationsObservable );
//
//        migrationManager.migrate();
//
//        verify( serialization, never() ).setStatusMessage( any( String.class ) );
//        verify( serialization, never() ).setStatusCode( any( Integer.class ) );
//        verify( serialization, never() ).setVersion( any( Integer.class ) );
//    }
//
//
//    @Test
//    public void multipleMigrations() throws Throwable {
//        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );
//        when(serialization.getCurrentVersion()).thenReturn(1);
//
//
//        final ApplicationDataMigration v1 = mock( ApplicationDataMigration.class );
//        when( v1.getVersion() ).thenReturn( 2 );
//        when( v1.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());
//
//        final ApplicationDataMigration v2 = mock( ApplicationDataMigration.class );
//        when( v2.getVersion() ).thenReturn( 3 );
//        when(v2.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());
//
//
//        Set<DataMigration> migrations = new HashSet<>();
//        migrations.add( v1 );
//        migrations.add( v2 );
//
//
//
//        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, migrations,allEntitiesInSystemObservable,allApplicationsObservable );
//
//        migrationManager.migrate();
//
//
//        verify( v1 ).migrate(any(Observable.class), any( DataMigration.ProgressObserver.class ) );
//        verify( v2 ).migrate(any(Observable.class), any( DataMigration.ProgressObserver.class ) );
//
//        //verify we set the running status
//        verify( serialization, times( 2 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );
//
//        //set the status message
//        verify( serialization, times( 2 * 2 ) ).setStatusMessage( any( String.class ) );
//
//        verify( serialization ).setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );
//
//        //verify we set version 1
//        verify( serialization ).setVersion( 2 );
//
//        //verify we set version 2
//        verify( serialization ).setVersion( 3 );
//    }
//
//
//    @Test
//    public void shortCircuitVersionFails() throws Throwable {
//        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );
//        when(serialization.getCurrentVersion()).thenReturn(1);
//
//
//        final ApplicationDataMigration v1 = mock( ApplicationDataMigration.class,"mock1" );
//        when( v1.getVersion() ).thenReturn( 2 );
//        when( v1.getType() ).thenReturn(DataMigration.MigrationType.Entities);
//        when( v1.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());
//
//        //throw an exception
//        when( v1.migrate(any(Observable.class),
//                any(DataMigration.ProgressObserver.class) )).thenThrow(new RuntimeException( "Something bad happened" ));
//
//        final ApplicationDataMigration v2 = mock( ApplicationDataMigration.class,"mock2" );
//        when( v2.getType() ).thenReturn(DataMigration.MigrationType.Entities);
//        when( v2.getVersion() ).thenReturn( 3 );
//
//        Set<DataMigration> migrations = new HashSet<>();
//        migrations.add( v1 );
//        migrations.add( v2 );
//
//        DataMigrationManagerImpl migrationManager
//            = new DataMigrationManagerImpl( serialization, migrations,allEntitiesInSystemObservable,allApplicationsObservable );
//
//        migrationManager.migrate();
//
//
//        verify( v1 ).migrate( any(Observable.class),any( DataMigration.ProgressObserver.class ) );
//
//        //verify we don't run migration
//        verify( v2, never() ).migrate( any(Observable.class),any( DataMigration.ProgressObserver.class ) );
//
//        //verify we set the running status
//        verify( serialization, times( 1 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );
//
//        //set the status message
//        verify( serialization, times( 2 ) ).setStatusMessage( any( String.class ) );
//
//        //verify we set an error
//        verify( serialization ).setStatusCode( DataMigrationManagerImpl.StatusCode.ERROR.status );
//
//        //verify we never set version 1
//        verify( serialization, never() ).setVersion( 1 );
//
//        //verify we never set version 2
//        verify( serialization, never() ).setVersion( 2 );
//    }
//
//
//    @Test
//    public void failStopsProgress() throws Throwable {
//        final MigrationInfoSerialization serialization = mock(MigrationInfoSerialization.class);
//        when(serialization.getCurrentVersion()).thenReturn(1);
//
//        final CollectionDataMigration v1 = mock( CollectionDataMigration.class );
//        when( v1.getVersion() ).thenReturn( 2 );
//        when( v1.getType() ).thenReturn(DataMigration.MigrationType.Entities);
//        when( v1.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());
//
//        final int returnedCode = 100;
//
//        final String reason = "test reason";
//
//        //mark as fail but don't
//        when(v1.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenAnswer(
//            new Answer<Object>() {
//                @Override
//                public Object answer(final InvocationOnMock invocation) throws Throwable {
//                    final DataMigration.ProgressObserver progressObserver =
//                        (DataMigration.ProgressObserver) invocation.getArguments()[1];
//
//                    progressObserver.failed(returnedCode, reason);
//                    return null;
//                }
//            }
//
//        );
//
//        final CollectionDataMigration v2 = mock( CollectionDataMigration.class );
//        when( v2.getVersion() ).thenReturn( 3 );
//        when( v2.getType() ).thenReturn(DataMigration.MigrationType.Entities);
//        when(v2.migrate(any(Observable.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());
//
//        Set<DataMigration> applicationDataMigrations = new HashSet<>();
//        applicationDataMigrations.add( v1 );
//        applicationDataMigrations.add(v2);
//
//
//        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, applicationDataMigrations,allEntitiesInSystemObservable, allApplicationsObservable );
//
//        migrationManager.migrate();
//
//
//        verify( v1 ).migrate(any(Observable.class), any( DataMigration.ProgressObserver.class ) );
//
//        //verify we don't run migration
//        verify( v2, never() ).migrate( any(Observable.class),any( DataMigration.ProgressObserver.class ) );
//
//        //verify we set the running status
//        verify( serialization, times( 1 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );
//
//        //set the status message
//        verify( serialization ).setStatusMessage( "Migration version 2.  Starting migration" );
//
//        verify( serialization ).setStatusMessage( "Migration version 100.  Failed to migrate, reason is appended.  Error 'test reason'" );
//
//        //verify we set an error
//        verify( serialization, times(2) ).setStatusCode( DataMigrationManagerImpl.StatusCode.ERROR.status );
//
//        //verify we never set version 1
//        verify( serialization, never() ).setVersion( 1 );
//
//        //verify we never set version 2
//        verify( serialization, never() ).setVersion( 2 );
//    }
}
