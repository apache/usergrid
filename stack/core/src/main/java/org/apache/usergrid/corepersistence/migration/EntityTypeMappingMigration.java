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
import org.apache.usergrid.persistence.core.scope.ApplicationEntityGroup;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.scope.EntityIdScope;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Migration to ensure that our entity id is written into our map data
 */
public class EntityTypeMappingMigration implements DataMigration {

    private final ManagerCache managerCache;
    private final AllEntitiesInSystemObservable allEntitiesInSystemObservable;


    @Inject
    public EntityTypeMappingMigration( final ManagerCache managerCache, final AllEntitiesInSystemObservable allEntitiesInSystemObservable) {
       this.managerCache = managerCache;
        this.allEntitiesInSystemObservable = allEntitiesInSystemObservable;
    }


    @Override
    public Observable migrate(final ApplicationEntityGroup applicationEntityGroup, final ProgressObserver observer) throws Throwable {

        final AtomicLong atomicLong = new AtomicLong();

        final MapScope ms = CpNamingUtils.getEntityTypeMapScope(applicationEntityGroup.applicationScope.getApplication());

        final MapManager mapManager = managerCache.getMapManager(ms);
        return Observable.from(applicationEntityGroup.entityIds)
            .subscribeOn(Schedulers.io())
            .map(new Func1<EntityIdScope, Long>() {
                @Override
                public Long call(EntityIdScope idScope) {
                    final UUID entityUuid = idScope.getId().getUuid();
                    final String entityType = idScope.getId().getType();

                    mapManager.putString(entityUuid.toString(), entityType);

                    if (atomicLong.incrementAndGet() % 100 == 0) {
                        updateStatus(atomicLong, observer);
                    }
                    return atomicLong.get();
                }
            });
    }


    private void updateStatus( final AtomicLong counter, final ProgressObserver observer ) {

        observer.update( getVersion(), String.format( "Updated %d entities", counter.get() ) );
    }


    @Override
    public int getVersion() {
        return Versions.VERSION_1;
    }

    @Override
    public MigrationType getType() {
        return MigrationType.Entities;
    }
}
