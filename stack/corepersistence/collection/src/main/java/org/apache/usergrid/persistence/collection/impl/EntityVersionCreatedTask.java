/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.impl;


import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Fires events so that all EntityVersionCreated handlers area called.
 */
public class EntityVersionCreatedTask implements Task<Void> {
    private static final Logger logger = LoggerFactory.getLogger( EntityVersionCreatedTask.class );

    private Set<EntityVersionCreated> listeners;
    private final ApplicationScope collectionScope;
    private final Entity entity;


    @Inject
    public EntityVersionCreatedTask( @Assisted final ApplicationScope collectionScope,
                                     final Set<EntityVersionCreated> listeners,
                                     @Assisted final Entity entity ) {

        this.listeners = listeners;
        this.collectionScope = collectionScope;
        this.entity = entity;
    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
        logger.error( "Unable to run update task for collection {} with entity {} and version {}",
                new Object[] { collectionScope, entity}, throwable );
    }


    @Override
    public Void rejected() {

        // Our task was rejected meaning our queue was full.
        // We need this operation to run, so we'll run it in our current thread
        try {
            call();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Exception thrown in call task", e );
        }

        return null;
    }


    @Override
    public Void call() throws Exception {

        fireEvents();
        return null;
    }


    private void fireEvents() {

        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.iterator().next().versionCreated( collectionScope, entity );
            return;
        }

        logger.debug( "Started firing {} listeners", listenerSize );


        Observable.from( listeners )
                  .flatMap( currentListener -> Observable.just( currentListener ).doOnNext( listener -> {
                      listener.versionCreated( collectionScope, entity );
                  } ).subscribeOn( Schedulers.io() ), 10 ).toBlocking().last();


        logger.debug( "Finished firing {} listeners", listenerSize );
    }
}
