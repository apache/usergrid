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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.FailedEntityImport;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class FileImportStatisticsTest {

    @Test
    public void testSuccess() throws Exception {

        final EntityManager em = mock( EntityManager.class );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );


        final FileImportStatistics fileImportStatistics = new FileImportStatistics( importFileId, em );

        final long expectedCount = 100;

        for ( long i = 0; i < expectedCount; i++ ) {
            fileImportStatistics.entityWritten();
        }


        fileImportStatistics.complete();


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );


        assertEquals( "Same count expected", expectedCount, updated.getImportedEntityCount() );

        assertNull( updated.getErrorMessage() );
    }


    @Test
    public void testBoth() throws Exception {

        final EntityManager em = mock( EntityManager.class );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );

        //mock up returning the FailedEntityImport instance after save is invoked.

        when( em.create( any( FailedEntityImport.class ) ) ).thenAnswer( new Answer<FailedEntityImport>() {
            @Override
            public FailedEntityImport answer( final InvocationOnMock invocation ) throws Throwable {
                return ( FailedEntityImport ) invocation.getArguments()[0];
            }
        } );

        final FileImportStatistics fileImportStatistics = new FileImportStatistics( importFileId, em );

        final long expectedSuccess = 100;

        for ( long i = 0; i < expectedSuccess; i++ ) {
            fileImportStatistics.entityWritten();
        }

        final int expectedFails = 10;

        for ( int i = 0; i < expectedFails; i++ ) {
            fileImportStatistics.entityFailed( "Failed to write entity " + i );
        }


        fileImportStatistics.complete();


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );

        assertEquals( "Same count expected", expectedSuccess, updated.getImportedEntityCount() );

        assertEquals( "Same fail expected", expectedFails, updated.getFailedEntityCount() );

        assertEquals( "Correct error message",
            "Failed to import " + expectedFails + " entities.  Successfully imported " + expectedSuccess + " entities",
            updated.getErrorMessage() );

        //TODO get the connections from the file import

        ArgumentCaptor<FailedEntityImport> failedEntities = ArgumentCaptor.forClass( FailedEntityImport.class );

        verify( em, times( expectedFails ) )
            .createConnection( same( fileImport ), eq( "errors" ), failedEntities.capture() );

        //now check all our arguments

        final List<FailedEntityImport> args = failedEntities.getAllValues();

        assertEquals( "Same number of error connections created", expectedFails, args.size() );


        for ( int i = 0; i < expectedFails; i++ ) {

            final FailedEntityImport failedImport = args.get( i );

            assertEquals( "Same message expected", "Failed to write entity " + i, failedImport.getErrorMessage() );
        }
    }


    @Test
    public void explicitFail() throws Exception {

        final EntityManager em = mock( EntityManager.class );

        final UUID importFileId = UUIDGenerator.newTimeUUID();


        final FileImport fileImport = new FileImport();
        fileImport.setUuid( importFileId );

        when( em.get( importFileId, FileImport.class ) ).thenReturn( fileImport );


        final FileImportStatistics fileImportStatistics = new FileImportStatistics( importFileId, em );

        final long expectedCount = 100;

        for ( long i = 0; i < expectedCount; i++ ) {
            fileImportStatistics.entityWritten();
        }


        fileImportStatistics.fatal( "Something bad happened" );


        ArgumentCaptor<FileImport> savedFileImport = ArgumentCaptor.forClass( FileImport.class );

        verify( em ).update( savedFileImport.capture() );

        final FileImport updated = savedFileImport.getValue();

        assertSame( "Same instance should be updated", fileImport, updated );


        assertEquals( "Same count expected", expectedCount, updated.getImportedEntityCount() );

        assertEquals("Fail count is 0", 0, updated.getFailedEntityCount());

        assertEquals("Correct expected message", "Something bad happened", updated.getErrorMessage());

        assertEquals("Expected failed state", FileImport.State.FAILED, updated.getState());
    }
}

