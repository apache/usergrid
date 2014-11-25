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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.functions.Action1;


/**
 * Migration to ensure that our entity id is written into our map data
 */
public class EntityTypeMappingMigration implements DataMigration {

    private final ManagerCache managerCache;



    @Inject
    public EntityTypeMappingMigration( final ManagerCache managerCache) {
       this.managerCache = managerCache;
    }


    @Override
    public void migrate( final ProgressObserver observer ) throws Throwable {

        final AtomicLong atomicLong = new AtomicLong();

        AllEntitiesInSystemObservable.getAllEntitiesInSystem(managerCache, 1000 )
                                     .doOnNext( new Action1<AllEntitiesInSystemObservable.ApplicationEntityGroup>() {


                                         @Override
                                         public void call( final AllEntitiesInSystemObservable.ApplicationEntityGroup applicationEntityGroup ) {

                                             final MapScope ms = CpNamingUtils.getEntityTypeMapScope( applicationEntityGroup.applicationScope.getApplication() );


                                             final MapManager mapManager = managerCache.getMapManager( ms );

                                             for(Id entityId: applicationEntityGroup.entityIds) {
                                                 final UUID entityUuid = entityId.getUuid();
                                                 final String entityType = entityId.getType();

                                                 mapManager.putString( entityUuid.toString(), entityType );

                                                 if ( atomicLong.incrementAndGet() % 100 == 0 ) {
                                                     updateStatus( atomicLong, observer );
                                                 }
                                             }
                                         }
                                     } ).toBlocking().lastOrDefault( null );
    }


    private void updateStatus( final AtomicLong counter, final ProgressObserver observer ) {

        observer.update( getVersion(), String.format( "Updated %d entities", counter.get() ) );
    }


    @Override
    public int getVersion() {
        return Versions.VERSION_1;
    }
}
