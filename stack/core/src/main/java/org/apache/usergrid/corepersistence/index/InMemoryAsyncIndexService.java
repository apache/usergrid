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

package org.apache.usergrid.corepersistence.index;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.schedulers.Schedulers;


@Singleton
public class InMemoryAsyncIndexService implements AsyncIndexService {

    private final IndexService indexService;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;


    @Inject
    public InMemoryAsyncIndexService( final IndexService indexService,
                                      final EntityCollectionManagerFactory entityCollectionManagerFactory ) {this.indexService = indexService;


        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
    }


    @Override
    public void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Id entityId,
                                        final UUID version ) {

        final IndexEntityEvent event = new IndexEntityEvent( applicationScope, entityId, version );

        //process the entity immediately
        //only process the same version, otherwise ignore

        getEntity( applicationScope, entityId).filter( entity-> version.equals(entity.hasVersion() )).doOnNext( entity -> {
           indexService.indexEntity( applicationScope, entity );
        } ).subscribeOn( Schedulers.io() ).subscribe();


    }

    private Observable<Entity> getEntity( final ApplicationScope applicationScope, final Id entityId){

        final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager( applicationScope );
        return ecm.load( entityId );
    }
}
