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

package org.apache.usergrid.corepersistence.migration;


import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;

import com.google.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Migration to ensure that our entity id is written into our map data
 */
public class EntityTypeMappingMigration implements DataMigration<EntityIdScope> {

    private final ManagerCache managerCache;
    private final MigrationDataProvider<EntityIdScope> allEntitiesInSystemObservable;


    @Inject
    public EntityTypeMappingMigration( final ManagerCache managerCache,
                                       final MigrationDataProvider<EntityIdScope> allEntitiesInSystemObservable ) {
        this.managerCache = managerCache;
        this.allEntitiesInSystemObservable = allEntitiesInSystemObservable;
    }


    @Override
    public int migrate( final int currentVersion, final MigrationDataProvider<EntityIdScope> migrationDataProvider,
                        final ProgressObserver observer ) {

        final AtomicLong atomicLong = new AtomicLong();


        //migrate up to 100 types simultaneously
        allEntitiesInSystemObservable.getData().flatMap( entityIdScope -> {
            return Observable.just( entityIdScope ).doOnNext( entityIdScopeObservable -> {
                final MapScope ms = CpNamingUtils
                                                 .getEntityTypeMapScope( entityIdScope.getApplicationScope().getApplication() );

                                             final MapManager mapManager = managerCache.getMapManager( ms );

                                             final UUID entityUuid = entityIdScope.getId().getUuid();
                                             final String entityType = entityIdScope.getId().getType();

                                             mapManager.putString( entityUuid.toString(), entityType );

                                             if ( atomicLong.incrementAndGet() % 100 == 0 ) {
                                                 observer.update( getMaxVersion(),
                                                     String.format( "Updated %d entities", atomicLong.get() ) );
                                             }

            } ).subscribeOn( Schedulers.io() );
        }, 100 ).count().toBlocking().last();


        return getMaxVersion();


    }


    @Override
    public boolean supports( final int currentVersion ) {
        //we move from the migration version fix to the current version
        return CoreDataVersions.INITIAL.getVersion() == currentVersion;
    }


    @Override
    public int getMaxVersion() {
        return CoreDataVersions.ID_MAP_FIX.getVersion();
    }
}
