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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;


/**
 * Statistics used to track a file import. Only 1 instance of this class should exist per file imported in the cluster.
 * There is a direct 1-1 mapping of the statistics provided here and the file import status
 */
public class FileImportStatistics {

    private final AtomicLong entitiesWritten = new AtomicLong( 0 );
    private final AtomicLong entitiesFailed = new AtomicLong( 0 );


    private final UUID fileImportId;
    private final EntityManager entityManager;


    public FileImportStatistics( final UUID fileImportId, final EntityManager entityManager ) {
        this.fileImportId = fileImportId;
        this.entityManager = entityManager;
    }


    /**
     * Invoke when an entity has been successfully written
     */
    public void entityWritten() {

        entitiesWritten.incrementAndGet();
    }


    /**
     * Invoke when an entity fails to write correctly
     */

    public void entityFailed( final String message ) {
        entitiesFailed.incrementAndGet();
    }


    /**
     * Invoke when the file is completed processing
     */
    public void complete() {

        final long failed = entitiesFailed.get();
        final long written = entitiesWritten.get();
        final FileImport.State state;
        final String message;

        if ( failed > 0 ) {
            state = FileImport.State.FAILED;
            message = "Successfully imported " + written + " entities";
        }
        else {
            state = FileImport.State.FINISHED;
            message = "Failed to import " + failed + " entities.  Successfully imported " + written + " entities";
        }

        updateFileImport( written, failed, state, message );
    }


    /**
     * Invoke when we halt the import with a fatal error that cannot be recovered.
     */
    public void fatal( final String message ) {

        final long failed = entitiesFailed.get();
        final long written = entitiesWritten.get();
        final FileImport.State state;

        if ( failed > 0 ) {
            state = FileImport.State.FAILED;
        }
        else {
            state = FileImport.State.FINISHED;
        }

        updateFileImport( written, failed, state, message );
    }


    /**
     * Update the file import status with the provided messages
     *
     * @param written The number of files written
     * @param failed The number of files failed
     * @param state The state to set into the import
     * @param message The message to set
     */
    private void updateFileImport( final long written, final long failed, final FileImport.State state,
                                   final String message ) {

        try {
            FileImport fileImport = entityManager.get( fileImportId, FileImport.class );

            if ( fileImport == null ) {
                throw new EntityNotFoundException( "Could not file FileImport with id " + fileImportId );
            }


            fileImport.setImportedEntityCount( written );
            fileImport.setFailedEntityCount( failed );
            fileImport.setState( state );
            fileImport.setErrorMessage( message );

            entityManager.update( fileImport );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to persist complete state", e );
        }
    }


    /**
     * Returns true if we should stop processing.  This will use the following logic
     *
     * We've attempted to import over 1k entities After 1k, we have over a 50% failure rate
     */
    public boolean stopProcessing() {

        //TODO Dave, George.  What algorithm should we use here?
        return false;
    }
}
