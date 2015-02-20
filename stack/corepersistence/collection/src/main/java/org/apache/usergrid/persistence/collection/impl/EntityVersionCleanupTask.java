/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.Set;
import org.apache.usergrid.persistence.core.guice.ProxyImpl;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Cleans up previous versions from the specified version. Note that this means the version
 * passed in the io event is retained, the range is exclusive.
 */
public class EntityVersionCleanupTask implements Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger( EntityVersionCleanupTask.class );

    private final Set<EntityVersionDeleted> listeners;

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final Keyspace keyspace;

    private final SerializationFig serializationFig;

    private final CollectionScope scope;
    private final Id entityId;
    private final UUID version;


    @Inject
    public EntityVersionCleanupTask(
        final SerializationFig serializationFig,
        final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
        @ProxyImpl final MvccEntitySerializationStrategy   entitySerializationStrategy,
        final UniqueValueSerializationStrategy  uniqueValueSerializationStrategy,
        final Keyspace                          keyspace,
        final Set<EntityVersionDeleted>         listeners, // MUST be a set or Guice will not inject
        @Assisted final CollectionScope         scope,
        @Assisted final Id                      entityId,
        @Assisted final UUID                    version ) {

        this.serializationFig = serializationFig;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.keyspace = keyspace;
        this.listeners = listeners;
        this.scope = scope;
        this.entityId = entityId;
        this.version = version;
    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
        logger.error( "Unable to run update task for collection {} with entity {} and version {}",
                new Object[] { scope, entityId, version }, throwable );
    }


    @Override
    public Void rejected() {
        //Our task was rejected meaning our queue was full.  We need this operation to run,
        // so we'll run it in our current thread
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
        //TODO Refactor this logic into a a class that can be invoked from anywhere
        //load every entity we have history of
        Observable<List<MvccEntity>> deleteFieldsObservable =
            Observable.create(new ObservableIterator<MvccEntity>("deleteColumns") {
                @Override
                protected Iterator<MvccEntity> getIterator() {
                    Iterator<MvccEntity> entities =  entitySerializationStrategy.loadDescendingHistory(
                        scope, entityId, version, 1000); // TODO: what fetchsize should we use here?
                    return entities;
                }
            })
            //buffer them for efficiency
            .skip(1)
            .buffer(serializationFig.getBufferSize()).doOnNext(
            new Action1<List<MvccEntity>>() {
                @Override
                public void call(final List<MvccEntity> mvccEntities) {
                    final MutationBatch batch = keyspace.prepareMutationBatch();
                    final MutationBatch entityBatch = keyspace.prepareMutationBatch();
                    final MutationBatch logBatch = keyspace.prepareMutationBatch();

                    for (MvccEntity mvccEntity : mvccEntities) {
                        if (!mvccEntity.getEntity().isPresent()) {
                            continue;
                        }

                        final UUID entityVersion = mvccEntity.getVersion();
                        final Entity entity = mvccEntity.getEntity().get();

                        //remove all unique fields from the index
                        for (final Field field : entity.getFields()) {
                            if (!field.isUnique()) {
                                continue;
                            }
                            final UniqueValue unique = new UniqueValueImpl( field, entityId, entityVersion);
                            final MutationBatch deleteMutation =
                                    uniqueValueSerializationStrategy.delete(scope,unique);
                            batch.mergeShallow(deleteMutation);
                        }

                        final MutationBatch entityDelete = entitySerializationStrategy
                                .delete(scope, entityId, mvccEntity.getVersion());
                        entityBatch.mergeShallow(entityDelete);
                        final MutationBatch logDelete = logEntrySerializationStrategy
                                .delete(scope, entityId, version);
                        logBatch.mergeShallow(logDelete);
                    }

                    try {
                        batch.execute();
                    } catch (ConnectionException e1) {
                        throw new RuntimeException("Unable to execute " +
                                "unique value " +
                                "delete", e1);
                    }
                    fireEvents(mvccEntities);
                    try {
                        entityBatch.execute();
                    } catch (ConnectionException e) {
                        throw new RuntimeException("Unable to delete entities in cleanup", e);
                    }

                    try {
                        logBatch.execute();
                    } catch (ConnectionException e) {
                        throw new RuntimeException("Unable to delete entities from the log", e);
                    }

                }
            }
        );

        final int removedCount = deleteFieldsObservable.count().toBlocking().last();

        logger.debug("Removed unique values for {} entities of entity {}",removedCount,entityId);

        return null;
    }


    private void fireEvents( final List<MvccEntity> versions ) {

        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.iterator().next().versionDeleted( scope, entityId, versions );
            return;
        }

        logger.debug( "Started firing {} listeners", listenerSize );

        //if we have more than 1, run them on the rx scheduler for a max of 8 operations at a time
        Observable.from( listeners )
            .parallel( new Func1<Observable<EntityVersionDeleted>, Observable<EntityVersionDeleted>>() {

                @Override
                public Observable<EntityVersionDeleted> call(
                        final Observable<EntityVersionDeleted> entityVersionDeletedObservable ) {

                    return entityVersionDeletedObservable.doOnNext( new Action1<EntityVersionDeleted>() {
                        @Override
                        public void call( final EntityVersionDeleted listener ) {
                            listener.versionDeleted( scope, entityId, versions );
                        }
                    } );
                }
            }, Schedulers.io() ).toBlocking().last();

        logger.debug( "Finished firing {} listeners", listenerSize );
    }
}



