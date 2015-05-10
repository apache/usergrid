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

package org.apache.usergrid.corepersistence.asyncevents;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.index.IndexService;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.index.impl.IndexOperationMessage;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


@Singleton
public class InMemoryAsyncEventService implements AsyncEventService {

    private static final Logger log = LoggerFactory.getLogger( InMemoryAsyncEventService.class );

    private final IndexService indexService;
    private final RxTaskScheduler rxTaskScheduler;
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final boolean resolveSynchronously;


    @Inject
    public InMemoryAsyncEventService( final IndexService indexService, final RxTaskScheduler rxTaskScheduler,
                                      final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                      boolean resolveSynchronously ) {
        this.indexService = indexService;
        this.rxTaskScheduler = rxTaskScheduler;
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.resolveSynchronously = resolveSynchronously;
    }


    @Override
    public void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Entity entity ) {

        //process the entity immediately
        //only process the same version, otherwise ignore


        log.debug( "Indexing  in app scope {} entity {}", entity, applicationScope );

        final Observable<IndexOperationMessage> edgeObservable = indexService.indexEntity( applicationScope, entity );


        run( edgeObservable );
    }


    @Override
    public void queueNewEdge( final ApplicationScope applicationScope, final Entity entity, final Edge newEdge ) {

        log.debug( "Indexing  in app scope {} with entity {} and new edge {}",
            new Object[] { entity, applicationScope, newEdge } );

        final Observable<IndexOperationMessage> edgeObservable =  indexService.indexEdge( applicationScope, entity, newEdge );

        run( edgeObservable );
    }


    @Override
    public void queueDeleteEdge( final ApplicationScope applicationScope, final Edge edge ) {
        log.debug( "Deleting in app scope {} with edge {} }", applicationScope, edge );

        final Observable<IndexOperationMessage> edgeObservable = indexService.deleteIndexEdge( applicationScope, edge );

        run( edgeObservable );
    }


    @Override
    public void queueEntityDelete( final ApplicationScope applicationScope, final Id entityId ) {
        log.debug( "Deleting entity id from index in app scope {} with entityId {} }", applicationScope, entityId );

        final Observable<IndexOperationMessage> edgeObservable =
            indexService.deleteEntityIndexes( applicationScope, entityId );

        //TODO chain graph operations here

        run( edgeObservable );
    }


    @Override
    public void index( final EntityIdScope entityIdScope ) {

        final ApplicationScope applicationScope = entityIdScope.getApplicationScope();

        final Id entityId = entityIdScope.getId();

        //load the entity
        entityCollectionManagerFactory.createCollectionManager( applicationScope ).load( entityId )
            //perform indexing on the task scheduler and start it
            .flatMap( entity -> indexService.indexEntity( applicationScope, entity ) )
            .subscribeOn( rxTaskScheduler.getAsyncIOScheduler() ).subscribe();
    }


    public void run( Observable<?> observable ) {
        //start it in the background on an i/o thread
        if ( !resolveSynchronously ) {
            observable.subscribeOn( rxTaskScheduler.getAsyncIOScheduler() ).subscribe();
        }
        else {
            observable.toBlocking().last();
        }
    }
}
