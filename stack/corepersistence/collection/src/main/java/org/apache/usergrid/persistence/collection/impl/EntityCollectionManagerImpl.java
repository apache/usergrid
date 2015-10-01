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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.UniqueCleanup;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.VersionCompact;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.MinMaxLogEntryIterator;
import org.apache.usergrid.persistence.collection.serialization.impl.MutableFieldSet;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.serializers.StringSerializer;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;


/**
 * Simple implementation.  Should perform  writes, delete and load. <p/> TODO: maybe refactor the stage operations into
 * their own classes for clarity and organization?
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );


    //start stages
    private final WriteStart writeStart;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;
    private final RollbackAction rollback;
    private final UniqueCleanup uniqueCleanup;
    private final VersionCompact versionCompact;


    //delete stages
    private final MarkStart markStart;
    private final MarkCommit markCommit;

    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    private final SerializationFig serializationFig;


    private final Keyspace keyspace;
    private final Timer writeTimer;
    private final Timer deleteTimer;
    private final Timer fieldIdTimer;
    private final Timer fieldEntityTimer;
    private final Timer updateTimer;
    private final Timer loadTimer;
    private final Timer getLatestTimer;

    private final ApplicationScope applicationScope;
    private final RxTaskScheduler rxTaskScheduler;


    @Inject
    public EntityCollectionManagerImpl( final WriteStart writeStart, final WriteUniqueVerify writeVerifyUnique,
                                        final WriteOptimisticVerify writeOptimisticVerify,
                                        final WriteCommit writeCommit, final RollbackAction rollback,
                                        final MarkStart markStart, final MarkCommit markCommit,
                                        final UniqueCleanup uniqueCleanup, final VersionCompact versionCompact,
                                        final MvccEntitySerializationStrategy entitySerializationStrategy,
                                        final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                        final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy,
                                        final Keyspace keyspace, final MetricsFactory metricsFactory,
                                        final SerializationFig serializationFig, final RxTaskScheduler rxTaskScheduler,
                                        @Assisted final ApplicationScope applicationScope ) {
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.uniqueCleanup = uniqueCleanup;
        this.versionCompact = versionCompact;
        this.serializationFig = serializationFig;
        this.rxTaskScheduler = rxTaskScheduler;

        ValidationUtils.validateApplicationScope( applicationScope );

        this.writeStart = writeStart;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.rollback = rollback;


        this.markStart = markStart;
        this.markCommit = markCommit;

        this.keyspace = keyspace;


        this.applicationScope = applicationScope;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
        this.writeTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.write");
        this.deleteTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.delete");
        this.fieldIdTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.fieldId");
        this.fieldEntityTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.fieldEntity");
        this.updateTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.update");
        this.loadTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.load");
        this.getLatestTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "base.latest");
    }


    @Override
    public Observable<Entity> write( final Entity entity ) {

        //do our input validation
        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final Id entityId = entity.getId();

        ValidationUtils.verifyIdentity( entityId );


        // create our observable and start the write
        final CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>( applicationScope, entity );

        Observable<CollectionIoEvent<MvccEntity>> observable = stageRunner( writeData, writeStart );


        final Observable<Entity> write = observable.map( writeCommit );

        return ObservableTimer.time( write, writeTimer );
    }


    @Override
    public Observable<Id> mark( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );

        Observable<Id> o = Observable.just( new CollectionIoEvent<>( applicationScope, entityId ) ).map( markStart )
            .doOnNext( markCommit ).compose( uniqueCleanup ).map(
                entityEvent -> entityEvent.getEvent().getId() );


        return ObservableTimer.time( o, deleteTimer );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in load stage" );

        final Observable<Entity> entityObservable = load( Collections.singleton( entityId ) ).flatMap( entitySet -> {
            final MvccEntity entity = entitySet.getEntity( entityId );

            if ( entity == null || !entity.getEntity().isPresent() ) {
                return Observable.empty();
            }

            return Observable.just( entity.getEntity().get() );
        } );


        return ObservableTimer.time( entityObservable, loadTimer );
    }


    @Override
    public Observable<EntitySet> load( final Collection<Id> entityIds ) {

        Preconditions.checkNotNull( entityIds, "entityIds cannot be null" );

        final Observable<EntitySet> entitySetObservable = Observable.create( new Observable.OnSubscribe<EntitySet>() {

            @Override
            public void call( final Subscriber<? super EntitySet> subscriber ) {
                try {
                    final EntitySet results =
                        entitySerializationStrategy.load( applicationScope, entityIds, UUIDGenerator.newTimeUUID() );

                    subscriber.onNext( results );
                    subscriber.onCompleted();
                }
                catch ( Exception e ) {
                    subscriber.onError( e );
                }
            }
        } );


        return ObservableTimer.time( entitySetObservable, loadTimer );
    }


    @Override
    public Observable<MvccLogEntry> getVersions( final Id entityId ) {
        ValidationUtils.verifyIdentity( entityId );

        return Observable.create( new ObservableIterator<MvccLogEntry>( "Log entry iterator" ) {
            @Override
            protected Iterator<MvccLogEntry> getIterator() {
                return new MinMaxLogEntryIterator( mvccLogEntrySerializationStrategy, applicationScope, entityId,
                    serializationFig.getBufferSize() );
            }
        } );
    }


    @Override
    public Observable<MvccLogEntry> delete( final Collection<MvccLogEntry> entries ) {
        Preconditions.checkNotNull( entries, "entries must not be null" );


        return Observable.from( entries ).map( logEntry -> new CollectionIoEvent<>( applicationScope, logEntry ) )
            .compose( versionCompact ).map( event -> event.getEvent() );
    }


    @Override
    public Observable<Id> getIdField( final String type, final Field field ) {
        final List<Field> fields = Collections.singletonList( field );
        final Observable<Id> idObservable = Observable.from( fields ).map( field1 -> {
            try {
                final UniqueValueSet set = uniqueValueSerializationStrategy.load( applicationScope, type, fields );
                final UniqueValue value = set.getValue( field1.getName() );
                return value == null ? null : value.getEntityId();
            }
            catch ( ConnectionException e ) {
                logger.error( "Failed to getIdField", e );
                throw new RuntimeException( e );
            }
        } );

        return ObservableTimer.time( idObservable, fieldIdTimer );
    }


    /**
     * Retrieves all entities that correspond to each field given in the Collection.
     */
    @Override
    public Observable<FieldSet> getEntitiesFromFields( final String type, final Collection<Field> fields ) {
        final Observable<FieldSet> fieldSetObservable = Observable.just( fields ).map( fields1 -> {
            try {

                final UUID startTime = UUIDGenerator.newTimeUUID();

                //Get back set of unique values that correspond to collection of fields
                UniqueValueSet set = uniqueValueSerializationStrategy.load( applicationScope, type, fields1 );

                //Short circuit if we don't have any uniqueValues from the given fields.
                if ( !set.iterator().hasNext() ) {
                    return new MutableFieldSet( 0 );
                }


                //loop through each field, and construct an entity load
                List<Id> entityIds = new ArrayList<>( fields1.size() );
                List<UniqueValue> uniqueValues = new ArrayList<>( fields1.size() );

                for ( final Field expectedField : fields1 ) {

                    UniqueValue value = set.getValue( expectedField.getName() );

                    if ( value == null ) {
                        logger.debug( "Field does not correspond to a unique value" );
                    }

                    entityIds.add( value.getEntityId() );
                    uniqueValues.add( value );
                }

                //Load a entity for each entityId we retrieved.
                final EntitySet entitySet = entitySerializationStrategy.load( applicationScope, entityIds, startTime );

                //now loop through and ensure the entities are there.
                final MutationBatch deleteBatch = keyspace.prepareMutationBatch();

                final MutableFieldSet response = new MutableFieldSet( fields1.size() );

                for ( final UniqueValue expectedUnique : uniqueValues ) {
                    final MvccEntity entity = entitySet.getEntity( expectedUnique.getEntityId() );

                    //bad unique value, delete this, it's inconsistent
                    if ( entity == null || !entity.getEntity().isPresent() ) {
                        final MutationBatch valueDelete =
                            uniqueValueSerializationStrategy.delete( applicationScope, expectedUnique );
                        deleteBatch.mergeShallow( valueDelete );
                        continue;
                    }


                    //else add it to our result set
                    response.addEntity( expectedUnique.getField(), entity );
                }

                //TODO: explore making this an Async process
                //We'll repair it again if we have to
                deleteBatch.execute();

                return response;
            }
            catch ( ConnectionException e ) {
                logger.error( "Failed to getIdField", e );
                throw new RuntimeException( e );
            }
        } );


        return ObservableTimer.time( fieldSetObservable, fieldEntityTimer );
    }


    // fire the stages
    public Observable<CollectionIoEvent<MvccEntity>> stageRunner( CollectionIoEvent<Entity> writeData,
                                                                  WriteStart writeState ) {

        return Observable.just( writeData ).map( writeState ).flatMap( mvccEntityCollectionIoEvent -> {

            Observable<CollectionIoEvent<MvccEntity>> uniqueObservable =
                Observable.just( mvccEntityCollectionIoEvent ).subscribeOn( rxTaskScheduler.getAsyncIOScheduler() )
                    .doOnNext( writeVerifyUnique );


            // optimistic verification
            Observable<CollectionIoEvent<MvccEntity>> optimisticObservable =
                Observable.just( mvccEntityCollectionIoEvent ).subscribeOn( rxTaskScheduler.getAsyncIOScheduler() )
                    .doOnNext( writeOptimisticVerify );

            final Observable<CollectionIoEvent<MvccEntity>> zip =
                Observable.zip( uniqueObservable, optimisticObservable, ( unique, optimistic ) -> optimistic );

            return zip;
        } );
    }


    @Override
    public Observable<VersionSet> getLatestVersion( final Collection<Id> entityIds ) {


        final Observable<VersionSet> observable =  Observable.create( new Observable.OnSubscribe<VersionSet>() {

            @Override
            public void call( final Subscriber<? super VersionSet> subscriber ) {
                try {
                    final VersionSet logEntries = mvccLogEntrySerializationStrategy
                        .load( applicationScope, entityIds, UUIDGenerator.newTimeUUID() );

                    subscriber.onNext( logEntries );
                    subscriber.onCompleted();
                }
                catch ( Exception e ) {
                    subscriber.onError( e );
                }
            }
        } );

        return ObservableTimer.time( observable, getLatestTimer );
    }


    @Override
    public Health getHealth() {

        try {
            ColumnFamily<String, String> CF_SYSTEM_LOCAL =
                new ColumnFamily<String, String>( "system.local", StringSerializer.get(), StringSerializer.get(),
                    StringSerializer.get() );

            OperationResult<CqlResult<String, String>> result =
                keyspace.prepareQuery( CF_SYSTEM_LOCAL ).withCql( "SELECT now() FROM system.local;" ).execute();

            if ( result.getResult().getRows().size() == 1 ) {
                return Health.GREEN;
            }
        }
        catch ( ConnectionException ex ) {
            logger.error( "Error connecting to Cassandra", ex );
        }

        return Health.RED;
    }
}
