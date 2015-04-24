/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.impl;


import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.MutationBatch;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Fires Cleanup Task
 */
public class EntityDeletedTask implements Task<Void> {
    private static final Logger LOG =  LoggerFactory.getLogger(EntityDeletedTask.class);

    private final EntityVersionTaskFactory entityVersionTaskFactory;
    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private final Set<EntityDeleted> listeners;
    private final ApplicationScope collectionScope;
    private final Id entityId;
    private final UUID version;


    @Inject
    public EntityDeletedTask(
        EntityVersionTaskFactory entityVersionTaskFactory,
        final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
        final MvccEntitySerializationStrategy entitySerializationStrategy,
        final Set<EntityDeleted>                listeners, // MUST be a set or Guice will not inject
        @Assisted final ApplicationScope  collectionScope,
        @Assisted final Id                      entityId,
        @Assisted final UUID                    version) {

        this.entityVersionTaskFactory = entityVersionTaskFactory;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.listeners = listeners;
        this.collectionScope = collectionScope;
        this.entityId = entityId;
        this.version = version;
    }


    @Override
    public void exceptionThrown(Throwable throwable) {
        LOG.error( "Unable to run update task for collection {} with entity {} and version {}",
                new Object[] { collectionScope, entityId, version }, throwable );
    }


    @Override
    public Void rejected() {
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

        entityVersionTaskFactory.getCleanupTask( collectionScope, entityId, version, true ).call();

        fireEvents();
        final MutationBatch entityDelete = entitySerializationStrategy.delete(collectionScope, entityId, version);
        final MutationBatch logDelete = logEntrySerializationStrategy.delete(collectionScope, entityId, version);

        entityDelete.execute();
        logDelete.execute();
//
        return null;
    }


    private void fireEvents() {
        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.iterator().next().deleted( collectionScope, entityId,version );
            return;
        }

        LOG.debug( "Started firing {} listeners", listenerSize );

        //if we have more than 1, run them on the rx scheduler for a max of 10 operations at a time
        Observable.from(listeners).flatMap( currentListener -> Observable.just( currentListener ).doOnNext( listener -> {
            listener.deleted( collectionScope, entityId, version );
        } ).subscribeOn( Schedulers.io() ), 10 ).toBlocking().last();

        LOG.debug( "Finished firing {} listeners", listenerSize );
    }


}
