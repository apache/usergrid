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
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.LogEntryIterator;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;


/**
 * Cleans up previous versions from the specified version. Note that this means the version passed in the io event is
 * retained, the range is exclusive.
 */
public class EntityVersionCleanupTask implements Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger( EntityVersionCleanupTask.class );

    private final Set<EntityVersionDeleted> listeners;

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final Keyspace keyspace;

    private final SerializationFig serializationFig;

    private final ApplicationScope scope;
    private final Id entityId;
    private final UUID version;
    private final boolean includeVersion;


    @Inject
    public EntityVersionCleanupTask(
        final SerializationFig serializationFig,
        final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
        final UniqueValueSerializationStrategy  uniqueValueSerializationStrategy,
        final Keyspace                          keyspace,
        final Set<EntityVersionDeleted>         listeners, // MUST be a set or Guice will not inject
        @Assisted final ApplicationScope scope,
        @Assisted final Id                      entityId,
        @Assisted final UUID                    version,
        @Assisted final boolean includeVersion) {

        this.serializationFig = serializationFig;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.keyspace = keyspace;
        this.listeners = listeners;
        this.scope = scope;
        this.entityId = entityId;
        this.version = version;

        this.includeVersion = includeVersion;
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
       //iterate all unique values
        final BlockingObservable<Long> uniqueValueCleanup =
                Observable.create( new ObservableIterator<UniqueValue>( "Unique value load" ) {
                    @Override
                    protected Iterator<UniqueValue> getIterator() {
                        return uniqueValueSerializationStrategy.getAllUniqueFields( scope, entityId );
                    }
                } )

                        //skip current versions
                        .skipWhile( new Func1<UniqueValue, Boolean>() {
                            @Override
                            public Boolean call( final UniqueValue uniqueValue ) {
                                return !includeVersion && version.equals( uniqueValue.getEntityVersion() );
                            }
                        } )
                                //buffer our buffer size, then roll them all up in a single batch mutation
                        .buffer( serializationFig.getBufferSize() ).doOnNext( new Action1<List<UniqueValue>>() {
                    @Override
                    public void call( final List<UniqueValue> uniqueValues ) {
                        final MutationBatch uniqueCleanupBatch = keyspace.prepareMutationBatch();


                        for ( UniqueValue value : uniqueValues ) {
                            uniqueCleanupBatch.mergeShallow( uniqueValueSerializationStrategy.delete( scope, value ) );
                        }

                        try {
                            uniqueCleanupBatch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to execute batch mutation", e );
                        }
                    }
                } ).subscribeOn( Schedulers.io() ).countLong().toBlocking();


        //start calling the listeners for remove log entries
        BlockingObservable<Long> versionsDeletedObservable =

                Observable.create( new ObservableIterator<MvccLogEntry>( "Log entry iterator" ) {
                    @Override
                    protected Iterator<MvccLogEntry> getIterator() {

                        return new LogEntryIterator( logEntrySerializationStrategy, scope, entityId, version,
                                serializationFig.getBufferSize() );
                    }
                } )
                        //skip current version
                        .skipWhile( new Func1<MvccLogEntry, Boolean>() {
                            @Override
                            public Boolean call( final MvccLogEntry mvccLogEntry ) {
                                return !includeVersion && version.equals( mvccLogEntry.getVersion() );
                            }
                        } )
                                //buffer them for efficiency
                        .buffer( serializationFig.getBufferSize() ).doOnNext( new Action1<List<MvccLogEntry>>() {
                    @Override
                    public void call( final List<MvccLogEntry> mvccEntities ) {

                        fireEvents( mvccEntities );

                        final MutationBatch logCleanupBatch = keyspace.prepareMutationBatch();


                        for ( MvccLogEntry entry : mvccEntities ) {
                            logCleanupBatch.mergeShallow( logEntrySerializationStrategy.delete( scope, entityId, entry.getVersion() ));
                        }

                        try {
                            logCleanupBatch.execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to execute batch mutation", e );
                        }
                    }
                } ).subscribeOn( Schedulers.io() ).countLong().toBlocking();

        //wait or this to complete
        final Long removedCount = uniqueValueCleanup.last();

        logger.debug( "Removed unique values for {} entities of entity {}", removedCount, entityId );

        final Long versionCleanupCount = versionsDeletedObservable.last();

        logger.debug( "Removed {} previous entity versions of entity {}", versionCleanupCount, entityId );

        return null;
    }


    private void fireEvents( final List<MvccLogEntry> versions ) {

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


        //if we have more than 1, run them on the rx scheduler for a max of 10 operations at a time
        Observable.from(listeners).flatMap( currentListener -> Observable.just( currentListener ).doOnNext( listener -> {
            listener.versionDeleted( scope, entityId, versions );
        } ).subscribeOn( Schedulers.io() ), 10 ).toBlocking().last();



        logger.debug( "Finished firing {} listeners", listenerSize );
    }
}



