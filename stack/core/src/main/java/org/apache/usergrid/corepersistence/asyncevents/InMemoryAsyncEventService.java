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

import org.apache.usergrid.corepersistence.index.EntityIndexOperation;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


/**
 * TODO refactor this implementation into another class. The AsyncEventService impl will then invoke this class
 *
 * Performs in memory asynchronous execution using a task scheduler to limit throughput via RX.
 */
@Singleton
public class InMemoryAsyncEventService implements AsyncEventService {

    private static final Logger log = LoggerFactory.getLogger( InMemoryAsyncEventService.class );

    private final EventBuilder eventBuilder;
    private final RxTaskScheduler rxTaskScheduler;
    private final boolean resolveSynchronously;


    @Inject
    public InMemoryAsyncEventService( final EventBuilder eventBuilder, final RxTaskScheduler rxTaskScheduler, boolean
        resolveSynchronously ) {
        this.eventBuilder = eventBuilder;
        this.rxTaskScheduler = rxTaskScheduler;
        this.resolveSynchronously = resolveSynchronously;
    }


    @Override
    public void queueEntityIndexUpdate( final ApplicationScope applicationScope, final Entity entity ) {

        //process the entity immediately
        //only process the same version, otherwise ignore


        run( eventBuilder.queueEntityIndexUpdate(applicationScope, entity) );
    }


    @Override
    public void queueNewEdge( final ApplicationScope applicationScope, final Entity entity, final Edge newEdge ) {
        run( eventBuilder.queueNewEdge(applicationScope, entity, newEdge) );
    }


    @Override
    public void queueDeleteEdge( final ApplicationScope applicationScope, final Edge edge ) {
        run( eventBuilder.queueDeleteEdge(applicationScope, edge) );
    }


    @Override
    public void queueEntityDelete( final ApplicationScope applicationScope, final Id entityId ) {

        final EventBuilderImpl.EntityDeleteResults results =
            eventBuilder.queueEntityDelete( applicationScope, entityId );

        run( results.getIndexObservable() );
        run( results.getEntitiesCompacted() );
    }


    @Override
    public void index( final ApplicationScope applicationScope, final Id id ) {
        run(eventBuilder.index(new EntityIndexOperation(applicationScope, id, Long.MAX_VALUE)));
    }


    public void run( Observable<?> observable ) {
        //start it in the background on an i/o thread
        if ( !resolveSynchronously ) {
            observable.subscribeOn( rxTaskScheduler.getAsyncIOScheduler() ).subscribe();
        }
        else {
            observable.toBlocking().lastOrDefault(null);
        }
    }
}
