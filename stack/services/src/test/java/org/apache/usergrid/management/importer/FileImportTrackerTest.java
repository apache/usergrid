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

package org.apache.usergrid.management.importer;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.FailedImport;
import org.apache.usergrid.persistence.entities.FailedImportConnection;
import org.apache.usergrid.persistence.entities.FailedImportEntity;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class FileImportTrackerTest {

    @Test
    public void testSuccess() throws Exception {

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        final UUID importFileId = UUIDGenerator.newTimeUUID();

        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );

        final FileImportTracker fileImportTracker = new FileImportTracker( emf, fileImport, 1000 );

        final long expectedCount = 100;

        for ( long i = 0; i < expectedCount; i++ ) {
            fileImportTracker.entityWritten();
        }


        fileImportTracker.complete();


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );


        assertEquals( "Same count expected", expectedCount, updated.getImportedEntityCount() );

        assertNull( updated.getErrorMessage() );
    }


    @Test
    public void testBoth() throws Exception {

        // create mock em and emf

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        // create tracker and call its entityWritten() and entityFailed() methods some number of times

        final long expectedSuccess = 100;
        final int expectedFails = 10;
        final FileImport fileImport = new FileImport();
        final UUID importFileId = UUIDGenerator.newTimeUUID();
        fileImport.setUuid(importFileId);

        // when tracker tries to create a FailedImportEntity, give it a mock
        when(em.create(any(FailedImportEntity.class))).thenAnswer(new Answer<FailedImportEntity>() {
            @Override
            public FailedImportEntity answer(final InvocationOnMock invocation) throws Throwable {
                return (FailedImportEntity) invocation.getArguments()[0];
            }
        });

        final FileImportTracker fileImportTracker = new FileImportTracker(emf, fileImport, 1000);
        for (long i = 0; i < expectedSuccess; i++) {
            fileImportTracker.entityWritten();
        }
        for (int i = 0; i < expectedFails; i++) {
            fileImportTracker.entityFailed("Failed to write entity " + i);
        }
        fileImportTracker.complete();


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );
        assertEquals( "Same count expected", expectedSuccess, updated.getImportedEntityCount() );
        assertEquals( "Same fail expected", expectedFails, updated.getFailedEntityCount() );

        // TODO why is error message not being set here?
//        assertEquals( "Correct error message",
//            "Failed to import some data.  See the import counters and errors.",
//            updated.getErrorMessage() );

        // TODO get the connections from the file import

        ArgumentCaptor<FailedImportEntity> failedEntities =
            ArgumentCaptor.forClass( FailedImportEntity.class );

        verify( em, times( expectedFails ) )
            .createConnection( same( fileImport ), eq( "errors" ), failedEntities.capture() );

        // now check all our arguments

        final List<FailedImportEntity> args = failedEntities.getAllValues();
        assertEquals( "Same number of error connections created", expectedFails, args.size() );


        for ( int i = 0; i < expectedFails; i++ ) {
            final FailedImportEntity failedImport = args.get( i );
            assertEquals( "Same message expected",
                "Failed to write entity " + i, failedImport.getErrorMessage() );
        }
    }


    @Test
    public void explicitFail() throws Exception {

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );


        final FileImportTracker fileImportTracker = new FileImportTracker( emf, fileImport, 1000 );

        final long expectedCount = 100;

        for ( long i = 0; i < expectedCount; i++ ) {
            fileImportTracker.entityWritten();
        }


        fileImportTracker.fatal( "Something bad happened" );


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );


        assertEquals( "Same count expected", expectedCount, updated.getImportedEntityCount() );

        assertEquals( "Fail count is 0", 0, updated.getFailedEntityCount() );

        assertEquals( "Correct expected message", "Something bad happened", updated.getErrorMessage() );

        assertEquals( "Expected failed state", FileImport.State.FAILED, updated.getState() );
    }


    @Test
    public void testAutoFlushSuccess() throws Exception {

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );

        //mock up returning the FailedEntityImport instance after save is invoked.


        when( em.create( any( FailedImportConnection.class ) ) ).thenAnswer( new Answer<Object>() {
            @Override
            public Object answer( final InvocationOnMock invocation ) throws Throwable {
                return invocation.getArguments()[0];
            }
        } );

        final int expectedSuccess = 100;
        final int expectedFails = 100;
        final int expectedConnectionSuccess = 100;
        final int expectedConnectionFails = 100;

        final int expectedFlushCount = 2;
        final int flushSize =
            ( expectedFails + expectedFails + expectedConnectionSuccess + expectedConnectionFails )
            / expectedFlushCount;

        //set this to 1/2, so that we get saved twice
        final FileImportTracker fileImportTracker = new FileImportTracker( emf, fileImport, flushSize );


        for ( long i = 0; i < expectedSuccess; i++ ) {
            fileImportTracker.entityWritten();
        }


        for ( int i = 0; i < expectedFails; i++ ) {
            fileImportTracker.entityFailed( "Failed to write entity " + i );
        }


        for ( long i = 0; i < expectedConnectionSuccess; i++ ) {
            fileImportTracker.connectionWritten();
        }


        for ( int i = 0; i < expectedConnectionFails; i++ ) {
            fileImportTracker.connectionFailed( "Failed to write connection " + i );
        }


        fileImportTracker.complete();


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em, times( expectedFlushCount + 1 ) ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getAllValues().get( 2 );

        assertSame( "Same instance should be updated", fileImport, updated );

        assertEquals( "Same count expected", expectedSuccess, updated.getImportedEntityCount() );

        assertEquals( "Same fail expected", expectedFails, updated.getFailedEntityCount() );

        assertEquals( "Same connection count expected", expectedConnectionSuccess,
            updated.getImportedConnectionCount() );

        assertEquals( "Same connection error count expected", expectedConnectionFails,
            updated.getFailedConnectionCount() );

        assertTrue(updated.getErrorMessage().startsWith("Failed to import") );

        //TODO get the connections from the file import

        ArgumentCaptor<FailedImport> failedEntities = ArgumentCaptor.forClass( FailedImport.class );

        verify( em, times( expectedFails + expectedConnectionFails ) )
            .createConnection( same( fileImport ), eq( "errors" ), failedEntities.capture() );

        //now check all our arguments

        final List<FailedImport> args = failedEntities.getAllValues();

        assertEquals( "Same number of error connections created",
            expectedFails + expectedConnectionFails, args.size() );


        for ( int i = 0; i < expectedFails; i++ ) {

            final FailedImport failedImport = args.get( i );

            assertEquals( "Same message expected",
                "Failed to write entity " + i, failedImport.getErrorMessage() );
        }

        for ( int i = expectedFails; i < expectedConnectionFails; i++ ) {

            final FailedImport failedImport = args.get( i );

            assertEquals( "Same message expected",
                "Failed to write connection " + i, failedImport.getErrorMessage() );
        }
    }


    @Test
    public void loadingExistingState() throws Exception {

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );
        fileImport.setImportedEntityCount( 1 );
        fileImport.setFailedEntityCount( 2 );
        fileImport.setImportedConnectionCount( 3 );
        fileImport.setFailedConnectionCount( 4 );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );

        //mock up returning the FailedEntityImport instance after save is invoked.

        FileImportTracker statistics = new FileImportTracker( emf, fileImport, 100 );

        assertEquals( 1, statistics.getEntitiesWritten() );
        assertEquals( 2, statistics.getEntitiesFailed() );

        assertEquals( 3, statistics.getTotalEntityCount() );

        assertEquals( 3, statistics.getConnectionsWritten() );
        assertEquals( 4, statistics.getConnectionsFailed() );

        assertEquals( 7, statistics.getTotalConnectionCount() );
    }


    @Test
    public void failFast() throws Exception {

        final EntityManagerFactory emf = mock( EntityManagerFactory.class );
        final EntityManager em = mock( EntityManager.class );
        when( emf.getEntityManager( emf.getManagementAppId() ) ).thenReturn( em );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );

        //mock up returning the FailedEntityImport instance after save is invoked.

        FileImportTracker statistics = new FileImportTracker( emf, fileImport, 100 );


        assertFalse( statistics.shouldStopProcessingEntities() );

        assertFalse( statistics.shouldStopProcessingConnections() );


        statistics.entityFailed( "test fail" );

        assertTrue("We shouldn't process after a failure", statistics.shouldStopProcessingEntities());

        statistics.connectionFailed( "test fail" );

        assertTrue( "We shouldn't process after a failure", statistics.shouldStopProcessingConnections() );
    }
}

