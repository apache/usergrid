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


import org.junit.Test;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.migration.data.TestMigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test for our entity type mapping
 */
public class EntityTypeMappingMigrationIT  {


    @Test
    public void testIdMapping() throws Throwable {

        final Id applicationId = createId("application");

        final ApplicationScope scope1 = new ApplicationScopeImpl( applicationId );

        final Id entityId1 = createId("thing");

        final EntityIdScope idScope1 = new EntityIdScope(scope1, entityId1 );

        final MapScope mapScope1 = new MapScopeImpl(applicationId, CpNamingUtils.TYPES_BY_UUID_MAP );



        final ApplicationScope scope2 = new ApplicationScopeImpl( applicationId);

        final Id entityId2 = createId("foo");

        final EntityIdScope idScope2 = new EntityIdScope( scope2, entityId2 );

        final MapScope mapScope2 = new MapScopeImpl(applicationId, CpNamingUtils.TYPES_BY_UUID_MAP );


        final Observable<EntityIdScope> scopes = Observable.just(idScope1, idScope2);

        final TestMigrationDataProvider<EntityIdScope> migrationDataProvider = new TestMigrationDataProvider<>();

        //set our scopes
        migrationDataProvider.setObservable( scopes );




        //mock up returning our map manager
        final MapManager mapManager = mock(MapManager.class);
        final ManagerCache managerCache = mock(ManagerCache.class);

        when(managerCache.getMapManager( eq( mapScope1 ) )).thenReturn( mapManager );

        when(managerCache.getMapManager( eq( mapScope2 ) )).thenReturn( mapManager );

        final TestProgressObserver progressObserver = new TestProgressObserver();


        //wire it up
        final EntityTypeMappingMigration migration = new EntityTypeMappingMigration( managerCache, migrationDataProvider );

        //run it

        final int returnedVersion = migration.migrate(CoreDataVersions.INITIAL.getVersion(), migrationDataProvider, progressObserver );


        assertEquals(CoreDataVersions.ID_MAP_FIX.getVersion(), returnedVersion);

        //verify we saved it

        verify(mapManager).putString(entityId1.getUuid().toString(), entityId1.getType()  );

        verify(mapManager).putString(entityId2.getUuid().toString(), entityId2.getType()  );










    }
}
