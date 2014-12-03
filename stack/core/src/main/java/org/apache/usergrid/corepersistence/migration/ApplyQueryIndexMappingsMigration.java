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


import java.util.concurrent.atomic.AtomicLong;


import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.persistence.core.migration.data.DataMigration;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.rx.ApplicationObservable;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;

import rx.functions.Action1;


/**
 * Migration applies the current Query Index mappings to every Query Index, 
 * and thus every Application in the system. 
 */
public class ApplyQueryIndexMappingsMigration implements DataMigration {
    
    private final ManagerCache managerCache;

    @Inject
    public ApplyQueryIndexMappingsMigration( final ManagerCache managerCache) {
       this.managerCache = managerCache;
    }


    @Override
    public void migrate( final ProgressObserver observer ) throws Throwable {

        ApplicationObservable.getAllApplicationIds( managerCache )
            .doOnNext( new Action1<Id>() {

                @Override
                public void call( final Id appId ) {
                    EntityIndex ei = managerCache.getEntityIndex(new ApplicationScopeImpl( appId ));
                    ei.initializeIndex();
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
