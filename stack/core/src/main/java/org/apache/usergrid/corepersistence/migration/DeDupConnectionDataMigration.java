/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.migration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.service.ConnectionService;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class DeDupConnectionDataMigration implements DataMigration {

    private static final Logger logger = LoggerFactory.getLogger(DeDupConnectionDataMigration.class);

    private static final long UPDATE_COUNT = 1000;

    private final ConnectionService connectionService;
    private final AllApplicationsObservable allApplicationsObservable;


    @Inject
    public DeDupConnectionDataMigration( final ConnectionService connectionService,
                                         final AllApplicationsObservable allApplicationsObservable ) {
        this.connectionService = connectionService;
        this.allApplicationsObservable = allApplicationsObservable;
    }


    @Override
    public int migrate( final int currentVersion, final ProgressObserver observer ) {

        final int migrationVersion = getMaxVersion();

        observer.start();

        connectionService.deDupeConnections( allApplicationsObservable.getData() ).reduce( 0l, ( count, deDuped ) -> {

            final long newCount = count + 1;

            /**
             * Update our progress observer
             */
            if ( newCount % UPDATE_COUNT == 0 ) {
                logger.info( "De duped {} edges", newCount );
                observer.update( migrationVersion, String.format( "De duped %d edges", newCount ) );
            }

            return newCount;
        } ).doOnNext( total -> {
            logger.info( "Completed de-duping {} edges", total );
            observer.complete();
        } ).subscribe(); //want this to run through all records

        return migrationVersion;

    }


    @Override
    public boolean supports( final int currentVersion ) {
        return currentVersion <= getMaxVersion() - 1;
    }


    @Override
    public int getMaxVersion() {
        //needs to be 2 b/c our obsolete EntityTypeMappingMigration was 1
        return 2;
    }
}
