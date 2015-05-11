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
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.corepersistence.util.SerializableMapper;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.observables.ConnectableObservable;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;


@Singleton
public class ReIndexServiceImpl implements ReIndexService {

    private static final MapScope RESUME_MAP_SCOPTE =
        new MapScopeImpl( CpNamingUtils.getManagementApplicationId(), "reindexresume" );

    //Keep cursors to resume re-index for 1 day.  This is far beyond it's useful real world implications anyway.
    private static final int INDEX_TTL = 60 * 60 * 24 * 10;


    private final AllApplicationsObservable allApplicationsObservable;
    private final AllEntityIdsObservable allEntityIdsObservable;
    private final IndexProcessorFig indexProcessorFig;
    private final RxTaskScheduler rxTaskScheduler;
    private final MapManager mapManager;
    private final AsyncEventService indexService;


    @Inject
    public ReIndexServiceImpl( final AllEntityIdsObservable allEntityIdsObservable,
                               final MapManagerFactory mapManagerFactory,
                               final AllApplicationsObservable allApplicationsObservable, final IndexProcessorFig indexProcessorFig,
                               final RxTaskScheduler rxTaskScheduler, final AsyncEventService indexService ) {
        this.allEntityIdsObservable = allEntityIdsObservable;
        this.allApplicationsObservable = allApplicationsObservable;
        this.indexProcessorFig = indexProcessorFig;
        this.rxTaskScheduler = rxTaskScheduler;
        this.indexService = indexService;

        this.mapManager = mapManagerFactory.createMapManager( RESUME_MAP_SCOPTE );
    }



    @Override
    public IndexResponse rebuildIndex( final Optional<UUID> appId, final Optional<String> collection, final Optional<String> cursor,
                                       final Optional<Long> startTimestamp ) {

        //load our last emitted Scope if a cursor is present
        if ( cursor.isPresent() ) {
            throw new UnsupportedOperationException( "Build this" );
        }


        final Observable<ApplicationScope>  applicationScopes = appId.isPresent()? Observable.just( getApplicationScope(appId.get()) ) : allApplicationsObservable.getData();

        final String newCursor = StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );

        //create an observable that loads each entity and indexes it, start it running with publish
        final ConnectableObservable<EdgeScope> runningReIndex =
            allEntityIdsObservable.getEdgesToEntities( applicationScopes, collection, startTimestamp )

                //for each edge, create our scope and index on it
                .doOnNext( edge -> indexService.index( new EntityIdScope( edge.getApplicationScope(), edge.getEdge().getTargetNode() ) ) ).publish();



        //start our sampler and state persistence
        //take a sample every sample interval to allow us to resume state with minimal loss
        runningReIndex.sample( indexProcessorFig.getReIndexSampleInterval(), TimeUnit.MILLISECONDS,
            rxTaskScheduler.getAsyncIOScheduler() )
            .doOnNext( edge -> {

                final String serializedState = SerializableMapper.asString( edge );

                mapManager.putString( newCursor, serializedState, INDEX_TTL );
            } ).subscribe();


        //start pushing to both
        runningReIndex.connect();


        return new IndexResponse( newCursor, runningReIndex );
    }
}


