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


import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
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


        final FileImportStatistics fileImportStatistics = new FileImportStatistics( importFileId, em );

        final long expectedSuccess = 100;

        for ( long i = 0; i < expectedSuccess; i++ ) {
            fileImportStatistics.entityWritten();
        }

        final long expectedFails = 10;

        for ( long i = 0; i < expectedFails; i++ ) {
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
    }
}
