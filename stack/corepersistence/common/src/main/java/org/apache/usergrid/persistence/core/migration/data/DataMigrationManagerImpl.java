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


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.schema.MigrationException;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class DataMigrationManagerImpl implements DataMigrationManager {

    private static final Logger LOG = LoggerFactory.getLogger( DataMigrationManagerImpl.class );

    private final TreeMap<Integer, DataMigration> migrationTreeMap = new TreeMap<>();

    private final MigrationInfoSerialization migrationInfoSerialization;


    @Inject
    public DataMigrationManagerImpl( final MigrationInfoSerialization migrationInfoSerialization,
                                     final Set<DataMigration> migrations ) {
        this.migrationInfoSerialization = migrationInfoSerialization;

        for ( DataMigration migration : migrations ) {

            final int version = migration.getVersion();

            final DataMigration existing = migrationTreeMap.get( version );

            if ( existing != null ) {

                final Class<? extends DataMigration> existingClass = existing.getClass();

                final Class<? extends DataMigration> currentClass = migration.getClass();


                throw new DataMigrationException(
                        String.format( "Data migrations must be unique.  Both classes %s and %s have version %d",
                                existingClass, currentClass, version ) );
            }

            migrationTreeMap.put( version, migration );
        }
    }


    @Override
    public void migrate() throws MigrationException {

        if ( migrationTreeMap.isEmpty() ) {
            LOG.warn( "No migrations found to run, exiting" );
            return;
        }


        final int currentVersion = migrationInfoSerialization.getVersion();

        LOG.info( "Saved schema version is {}, max migration version is {}", currentVersion,
                migrationTreeMap.lastKey() );

        //we have our migrations to run, execute them
        final NavigableMap<Integer, DataMigration> migrationsToRun = migrationTreeMap.tailMap( currentVersion, false );

        CassandraProgressObserver observer = new CassandraProgressObserver();


        for ( DataMigration migration : migrationsToRun.values() ) {

            migrationInfoSerialization.setStatusCode( StatusCode.RUNNING.status );

            final int migrationVersion = migration.getVersion();

            LOG.info( "Running migration version {}", migrationVersion );

            observer.update( migrationVersion,  "Starting migration" );


            //perform this migration, if it fails, short circuit
            try {
                migration.migrate( observer );
            }
            catch ( Throwable throwable ) {
                observer.failed( migrationVersion, "Exception thrown during migration", throwable );

                LOG.error( "Unable to migration version {}.", migrationVersion, throwable );

                return;
            }

            //we had an unhandled exception or the migration failed, short circuit
            if(observer.failed){
                return;
            }

            //set the version
            migrationInfoSerialization.setVersion( migrationVersion );

            //update the observer for progress so other nodes can see it
            observer.update( migrationVersion, "Completed successfully" );


        }

        migrationInfoSerialization.setStatusCode( StatusCode.COMPLETE.status );


    }


    @Override
    public boolean isRunning() {
        return migrationInfoSerialization.getStatusCode() == StatusCode.RUNNING.status;
    }


    @Override
    public String getLastStatus() {
        return migrationInfoSerialization.getStatusMessage();
    }


    /**
     * Different status enums
     */
    public enum StatusCode{
        COMPLETE(1),
        RUNNING(2),
        ERROR(3);

        public final int status;


        StatusCode( final int status ) {this.status = status;}
    }


    private final class CassandraProgressObserver implements DataMigration.ProgressObserver {

        private boolean failed = false;



        @Override
        public void failed( final int migrationVersion, final String reason ) {

            final String storedMessage = String.format( "Failed to migrate, reason is appended.  Error '%s'", reason);


            update(migrationVersion,  storedMessage );

            LOG.error( storedMessage );

            failed = true;

            migrationInfoSerialization.setStatusCode( StatusCode.ERROR.status );
        }


        @Override
        public void failed( final int migrationVersion, final String reason, final Throwable throwable ) {
            StringWriter stackTrace = new StringWriter();
            throwable.printStackTrace( new PrintWriter( stackTrace ) );


            final String storedMessage = String.format( "Failed to migrate, reason is appended.  Error '%s' %s", reason, stackTrace.toString() );

            update(migrationVersion,  storedMessage );


            LOG.error( "Unable to migrate version {} due to reason {}.", migrationVersion, reason, throwable );

            failed = true;

            migrationInfoSerialization.setStatusCode( StatusCode.ERROR.status );
        }


        @Override
        public void update( final int migrationVersion, final String message ) {
            final String error = String.format( "Migration version %d.  %s", migrationVersion, message);

            migrationInfoSerialization.setStatusMessage( error );
        }


        /**
         * Return true if we failed
         * @return
         */
        public boolean isFailed() {
            return failed;
        }
    }
}
