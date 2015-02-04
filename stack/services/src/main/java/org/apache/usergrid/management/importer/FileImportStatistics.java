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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.FailedEntityImport;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.exceptions.PersistenceException;


/**
 * Statistics used to track a file import. Only 1 instance of this class should exist per file imported in the cluster.
 * There is a direct 1-1 mapping of the statistics provided here and the file import status. This class is threadsafe to
 * be used across multiple threads.
 */
public class FileImportStatistics {


    private static final String ERRORS_CONNECTION_NAME = "errors";

    private final AtomicLong entitiesWritten = new AtomicLong( 0 );
    private final AtomicLong entitiesFailed = new AtomicLong( 0 );
    private final AtomicLong connectionsWritten = new AtomicLong( 0 );
    private final AtomicLong connectionsFailed = new AtomicLong( 0 );
    private final AtomicInteger cachedOperations = new AtomicInteger( 0 );

    private final Semaphore writeSemaphore = new Semaphore( 1 );

    private final FileImport fileImport;
    private final EntityManager entityManager;
    private final int flushCount;


    /**
     * Create an instance to track counters
     *
     * @param entityManager The entity manager that will hold these entities.
     * @param fileImportId The uuid of the fileImport
     * @param flushCount The number of success + failures to accumulate before flushing
     */
    public FileImportStatistics( final EntityManager entityManager, final UUID fileImportId, final int flushCount ) {
        this.entityManager = entityManager;
        this.flushCount = flushCount;
        this.fileImport = getFileImport( fileImportId );
    }


    /**
     * Invoke when an entity has been successfully written
     */
    public void entityWritten() {
        entitiesWritten.incrementAndGet();
        maybeFlush();
    }


    /**
     * Invoke when an entity fails to write correctly
     */

    public void entityFailed( final String message ) {
        entitiesFailed.incrementAndGet();


        FailedEntityImport failedEntityImport = new FailedEntityImport();
        failedEntityImport.setErrorMessage( message );

        try {
            failedEntityImport = entityManager.create( failedEntityImport );
            entityManager.createConnection( fileImport, ERRORS_CONNECTION_NAME, failedEntityImport );
        }
        catch ( Exception e ) {
            throw new PersistenceException( "Unable to save failed entity import message", e );
        }
        maybeFlush();
    }


    /**
     * Invoked when a connection is written
     */
    public void connectionWritten() {
        connectionsWritten.incrementAndGet();
        maybeFlush();
    }


    /**
     * Invoked when a connection cannot be written
     */
    public void connectionFailed( final String message ) {
        connectionsFailed.incrementAndGet();
        maybeFlush();
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
            message = "Failed to import " + failed + " entities.  Successfully imported " + written + " entities";
        }
        else {
            state = FileImport.State.FINISHED;
            message = null;
        }

        updateFileImport( written, failed, state, message );
    }


    /**
     * Invoke when we halt the import with a fatal error that cannot be recovered.
     */
    public void fatal( final String message ) {

        final long failed = entitiesFailed.get();
        final long written = entitiesWritten.get();

        updateFileImport( written, failed, FileImport.State.FAILED, message );
    }


    /**
     * Return the total number of successful imports + failed imports.  Can be used in resume. Note that this reflects
     * the counts last written to cassandra, NOT the current state in memory
     */
    public int getParsedEntityCount() {
        final FileImport saved = getFileImport( fileImport.getUuid() );

        //we could exceed an int.  if we do just truncate
        return ( int ) ( saved.getFailedEntityCount() + saved.getImportedEntityCount() );
    }


    /**
     * Returns true if we should stop processing.  This will use the following logic
     *
     * We've attempted to import over 1k entities After 1k, we have over a 50% failure rate
     */
    public boolean shouldStopProcessing() {

        //TODO Dave, George.  What algorithm should we use here?
        return false;
    }


    private void maybeFlush() {
        final int count = cachedOperations.incrementAndGet();

        //no op
        if ( count < flushCount ) {
            return;
        }

        //another thread is writing, no op, just return
        if ( !writeSemaphore.tryAcquire() ) {
            return;
        }

        final long failed = entitiesFailed.get();
        final long written = entitiesWritten.get();
        final String message;

        if ( failed > 0 ) {
            message = "Failed to import " + failed + " entities.  Successfully imported " + written + " entities";
        }
        else {
            message = "Successfully imported " + written + " entities";
        }

        updateFileImport( written, failed, FileImport.State.STARTED, message );
        cachedOperations.addAndGet( flushCount * -1 );
        writeSemaphore.release();
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
     * Get the FileImport by uuid and return it
     *
     * @throws EntityNotFoundException if we can't find the file import with the given uuid
     */
    private FileImport getFileImport( final UUID fileImportId ) {

        final FileImport fileImport;

        try {
            fileImport = entityManager.get( fileImportId, FileImport.class );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to load fileImport with id " + fileImportId, e );
        }

        if ( fileImport == null ) {
            throw new EntityNotFoundException( "Could not file FileImport with id " + fileImportId );
        }

        return fileImport;
    }
}
