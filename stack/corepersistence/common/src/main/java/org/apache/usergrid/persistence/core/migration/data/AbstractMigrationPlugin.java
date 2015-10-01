/*
 *
 *  *
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
 *  *
 *
 */

package org.apache.usergrid.persistence.core.migration.data;


import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Standard implementation logic for plugins to extend
 * @param <T>
 */
public abstract class AbstractMigrationPlugin implements MigrationPlugin {



    private static final Logger LOG = LoggerFactory.getLogger( AbstractMigrationPlugin.class );


    private final Set<DataMigration> entityDataMigrations;
    private final MigrationInfoSerialization migrationInfoSerialization;


    protected AbstractMigrationPlugin( final Set<DataMigration>entityDataMigrations,
                                       final MigrationInfoSerialization migrationInfoSerialization ) {
        this.entityDataMigrations = entityDataMigrations;
        this.migrationInfoSerialization = migrationInfoSerialization;
    }


    @Override
    public void run( final ProgressObserver observer ) {

        //run until complete
        while(runMigration( observer )){
         LOG.info( "Migration complete, checking for next run" );
        }

    }


    @Override
    public int getMaxVersion() {

        int max = 0;

        for(DataMigration entityMigration: entityDataMigrations){
            max = Math.max( max, entityMigration.getMaxVersion() );
        }

        return max;
    }


    /**
     * Try to run the migration
     *
     * @return True if we ran a migration
     */
    private boolean runMigration( final ProgressObserver po ) {
        DataMigration migrationToExecute = null;


        final int version = migrationInfoSerialization.getVersion( getName() );

        for ( DataMigration entityMigration : entityDataMigrations ) {
            if ( entityMigration.supports( version ) ) {
                if ( migrationToExecute != null ) {
                    throw new DataMigrationException(
                            "Two migrations attempted to migration the same version, this is not allowed.  Class '"
                                    + migrationToExecute.getClass().getName() + "' and class '" + entityMigration
                                    .getClass().getName()
                                    + "' both support this version. This means something is wired incorrectly" );
                }

                migrationToExecute = entityMigration;
            }
        }

        if(migrationToExecute == null){
            LOG.info( "No migrations found to execute" );
            return false;
        }

        po.start();


        //run the migration
        final int newSystemVersion = migrationToExecute.migrate( version, po );

        //write the version
        migrationInfoSerialization.setVersion( getName(), newSystemVersion );

        po.complete();

        //signal we've run a migration and return
        return true;


    }
}
