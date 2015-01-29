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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Observer;
import java.util.Set;

import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.model.entity.Id;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import rx.Observable;
import rx.Subscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests our data migration manager
 */
public class DataMigrationManagerImplTest {

    AllEntitiesInSystemObservable allEntitiesInSystemObservable = new AllEntitiesInSystemObservable() {
        @Override
        public Observable<ApplicationEntityGroup> getAllEntitiesInSystem(int bufferSize) {

            return Observable.create(new Observable.OnSubscribe<ApplicationEntityGroup>() {
                @Override
                public void call(Subscriber<? super ApplicationEntityGroup> subscriber) {
                    ApplicationEntityGroup entityGroup = new ApplicationEntityGroup(mock(ApplicationScope.class),new ArrayList<EntityIdScope>());
                    subscriber.onNext(entityGroup);
                    subscriber.onCompleted();
                }
            });
        }
    };

    @Test
    public void noMigrations() throws MigrationException {
        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );

        Set<DataMigration> emptyMigration = new HashSet<>();

        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, emptyMigration, allEntitiesInSystemObservable );

        migrationManager.migrate();

        verify( serialization, never() ).setStatusMessage( any( String.class ) );
        verify( serialization, never() ).setStatusCode( any( Integer.class ) );
        verify( serialization, never() ).setVersion( any( Integer.class ) );
    }


    @Test
    public void multipleMigrations() throws Throwable {
        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );


        final DataMigration v1 = mock( DataMigration.class );
        when( v1.getVersion() ).thenReturn( 1 );
        when( v1.migrate(any(ApplicationEntityGroup.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());

        final DataMigration v2 = mock( DataMigration.class );
        when( v2.getVersion() ).thenReturn( 2 );
        when(v2.migrate(any(ApplicationEntityGroup.class),any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());


        Set<DataMigration> migrations = new HashSet<>();
        migrations.add( v1 );
        migrations.add( v2 );


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, migrations,allEntitiesInSystemObservable );

        migrationManager.migrate();


        verify( v1 ).migrate(any(ApplicationEntityGroup.class), any( DataMigration.ProgressObserver.class ) );
        verify( v2 ).migrate(any(ApplicationEntityGroup.class), any( DataMigration.ProgressObserver.class ) );

        //verify we set the running status
        verify( serialization, times( 2 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );

        //set the status message
        verify( serialization, times( 2 * 2 ) ).setStatusMessage( any( String.class ) );

        verify( serialization ).setStatusCode( DataMigrationManagerImpl.StatusCode.COMPLETE.status );

        //verify we set version 1
        verify( serialization ).setVersion( 1 );

        //verify we set version 2
        verify( serialization ).setVersion( 2 );
    }


    @Test
    public void shortCircuitVersionFails() throws Throwable {
        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );


        final DataMigration v1 = mock( DataMigration.class );
        when( v1.getVersion() ).thenReturn( 1 );
        when( v1.migrate(any(ApplicationEntityGroup.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());

        //throw an exception
        doThrow( new RuntimeException( "Something bad happened" ) ).when( v1 ).migrate(any(ApplicationEntityGroup.class),
                any(DataMigration.ProgressObserver.class) );

        final DataMigration v2 = mock( DataMigration.class );
        when( v2.getVersion() ).thenReturn( 2 );


        Set<DataMigration> migrations = new HashSet<>();
        migrations.add( v1 );
        migrations.add( v2 );


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, migrations,allEntitiesInSystemObservable );

        migrationManager.migrate();


        verify( v1 ).migrate( any(ApplicationEntityGroup.class),any( DataMigration.ProgressObserver.class ) );

        //verify we don't run migration
        verify( v2, never() ).migrate( any(ApplicationEntityGroup.class),any( DataMigration.ProgressObserver.class ) );

        //verify we set the running status
        verify( serialization, times( 1 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );

        //set the status message
        verify( serialization, times( 2 ) ).setStatusMessage( any( String.class ) );

        //verify we set an error
        verify( serialization ).setStatusCode( DataMigrationManagerImpl.StatusCode.ERROR.status );

        //verify we never set version 1
        verify( serialization, never() ).setVersion( 1 );

        //verify we never set version 2
        verify( serialization, never() ).setVersion( 2 );
    }


    @Test
    public void failStopsProgress() throws Throwable {
        final MigrationInfoSerialization serialization = mock( MigrationInfoSerialization.class );

        final DataMigration v1 = mock( DataMigration.class );
        when( v1.getVersion() ).thenReturn( 1 );
        when( v1.migrate(any(ApplicationEntityGroup.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());

        final int returnedCode = 100;

        final String reason = "test reason";

        //mark as fail but don't
        when(v1.migrate(any(ApplicationEntityGroup.class), any(DataMigration.ProgressObserver.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    final DataMigration.ProgressObserver progressObserver =
                        (DataMigration.ProgressObserver) invocation.getArguments()[1];

                    progressObserver.failed(returnedCode, reason);
                    return null;
                }
            }

        );

        final DataMigration v2 = mock( DataMigration.class );
        when( v2.getVersion() ).thenReturn( 2 );
        when(v2.migrate(any(ApplicationEntityGroup.class), any(DataMigration.ProgressObserver.class))).thenReturn(Observable.empty());

        Set<DataMigration> migrations = new HashSet<>();
        migrations.add( v1 );
        migrations.add( v2 );


        DataMigrationManagerImpl migrationManager = new DataMigrationManagerImpl( serialization, migrations,allEntitiesInSystemObservable );

        migrationManager.migrate();


        verify( v1 ).migrate(any(ApplicationEntityGroup.class), any( DataMigration.ProgressObserver.class ) );

        //verify we don't run migration
        verify( v2, never() ).migrate( any(ApplicationEntityGroup.class),any( DataMigration.ProgressObserver.class ) );

        //verify we set the running status
        verify( serialization, times( 1 ) ).setStatusCode( DataMigrationManagerImpl.StatusCode.RUNNING.status );

        //set the status message
        verify( serialization ).setStatusMessage( "Migration version 1.  Starting migration" );

        verify( serialization ).setStatusMessage( "Migration version 100.  Failed to migrate, reason is appended.  Error 'test reason'" );

        //verify we set an error
        verify( serialization, times(2) ).setStatusCode( DataMigrationManagerImpl.StatusCode.ERROR.status );

        //verify we never set version 1
        verify( serialization, never() ).setVersion( 1 );

        //verify we never set version 2
        verify( serialization, never() ).setVersion( 2 );
    }
}
