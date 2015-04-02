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
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.FieldSet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.collection.guice.CollectionTaskExecutor;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.MutableFieldSet;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Meter;
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

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


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


    //delete stages
    private final MarkStart markStart;
    private final MarkCommit markCommit;

    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private final UniqueValueSerializationStrategy uniqueValueSerializationStrategy;

    private final EntityVersionTaskFactory entityVersionTaskFactory;
    private final TaskExecutor taskExecutor;

    private final Keyspace keyspace;
    private final Timer writeTimer;
    private final Meter writeMeter;
    private final Timer deleteTimer;
    private final Timer updateTimer;
    private final Timer loadTimer;
    private final Timer getLatestTimer;
    private final Meter deleteMeter;
    private final Meter getLatestMeter;
    private final Meter loadMeter;
    private final Meter updateMeter;

    private final ApplicationScope applicationScope;


    @Inject
    public EntityCollectionManagerImpl(
        final WriteStart                    writeStart,
        final WriteUniqueVerify                    writeVerifyUnique,
        final WriteOptimisticVerify                writeOptimisticVerify,
        final WriteCommit                          writeCommit,
        final RollbackAction                       rollback,
        final MarkStart                            markStart,
        final MarkCommit                           markCommit,
        final MvccEntitySerializationStrategy entitySerializationStrategy,
        final UniqueValueSerializationStrategy     uniqueValueSerializationStrategy,
        final MvccLogEntrySerializationStrategy    mvccLogEntrySerializationStrategy,
        final Keyspace                             keyspace,
        final EntityVersionTaskFactory entityVersionTaskFactory,
        @CollectionTaskExecutor final TaskExecutor taskExecutor,
        @Assisted final ApplicationScope applicationScope,
        final MetricsFactory metricsFactory

    ) {
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;

        ValidationUtils.validateApplicationScope( applicationScope );

        this.writeStart = writeStart;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.rollback = rollback;


        this.markStart = markStart;
        this.markCommit = markCommit;

        this.keyspace = keyspace;

        this.entityVersionTaskFactory = entityVersionTaskFactory;
        this.taskExecutor = taskExecutor;

        this.applicationScope = applicationScope;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
        this.writeTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class,"write.timer");
        this.writeMeter = metricsFactory.getMeter(EntityCollectionManagerImpl.class, "write.meter");
        this.deleteTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "delete.timer");
        this.deleteMeter= metricsFactory.getMeter(EntityCollectionManagerImpl.class, "delete.meter");
        this.updateTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class, "update.timer");
        this.updateMeter = metricsFactory.getMeter(EntityCollectionManagerImpl.class, "update.meter");
        this.loadTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class,"load.timer");
        this.loadMeter = metricsFactory.getMeter(EntityCollectionManagerImpl.class, "load.meter");
        this.getLatestTimer = metricsFactory.getTimer(EntityCollectionManagerImpl.class,"latest.timer");
        this.getLatestMeter = metricsFactory.getMeter(EntityCollectionManagerImpl.class, "latest.meter");
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

        // execute all validation stages concurrently.  Needs refactored when this is done.
        // https://github.com/Netflix/RxJava/issues/627
        // observable = Concurrent.concurrent( observable, Schedulers.io(), new WaitZip(),
        //                  writeVerifyUnique, writeOptimisticVerify );

        final Timer.Context timer = writeTimer.time();
        return observable.map(writeCommit).doOnNext(new Action1<Entity>() {
            @Override
            public void call(final Entity entity) {
                //TODO fire the created task first then the entityVersioncleanup
                taskExecutor.submit( entityVersionTaskFactory.getCreatedTask( applicationScope, entity ));
                taskExecutor.submit( entityVersionTaskFactory.getCleanupTask( applicationScope, entityId,
                    entity.getVersion(), false ));
                //post-processing to come later. leave it empty for now.
            }
        }).doOnError( rollback )
            .doOnEach( new Action1<Notification<? super Entity>>() {
                @Override
                public void call( Notification<? super Entity> notification ) {
                    writeMeter.mark();
                }
            } )
            .doOnCompleted( new Action0() {
                @Override
                public void call() {
                    timer.stop();
                }
            } );
    }


    @Override
    public Observable<Id> delete( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );

        final Timer.Context timer = deleteTimer.time();
        Observable<Id> o = Observable.just( new CollectionIoEvent<Id>( applicationScope, entityId ) )
            .map(markStart)
            .doOnNext( markCommit )
            .map(new Func1<CollectionIoEvent<MvccEntity>, Id>() {

                     @Override
                     public Id call(final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent) {
                         MvccEntity entity = mvccEntityCollectionIoEvent.getEvent();
                         Task<Void> task = entityVersionTaskFactory
                             .getDeleteTask( applicationScope, entity.getId(), entity.getVersion() );
                         taskExecutor.submit(task);
                         return entity.getId();
                     }
                 }
            )
            .doOnNext(new Action1<Id>() {
                @Override
                public void call(Id id) {
                    deleteMeter.mark();
                }
            })
            .doOnCompleted( new Action0() {
                @Override
                public void call() {
                    timer.stop();
                }
            } );

        return o;
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in load stage" );

        final Timer.Context timer = loadTimer.time();
        return load( Collections.singleton( entityId ) ).flatMap(new Func1<EntitySet, Observable<Entity>>() {
            @Override
            public Observable<Entity> call(final EntitySet entitySet) {
                final MvccEntity entity = entitySet.getEntity(entityId);

                if (entity == null || !entity.getEntity().isPresent()) {
                    return Observable.empty();
                }

                return Observable.just( entity.getEntity().get() );
            }
        })
            .doOnNext( new Action1<Entity>() {
                @Override
                public void call( Entity entity ) {
                    loadMeter.mark();
                }
            } )
            .doOnCompleted( new Action0() {
                @Override
                public void call() {
                    timer.stop();
                }
            } );
    }





    @Override
    public Observable<EntitySet> load( final Collection<Id> entityIds ) {

        Preconditions.checkNotNull( entityIds, "entityIds cannot be null" );

        final Timer.Context timer = loadTimer.time();

        return Observable.create( new Observable.OnSubscribe<EntitySet>() {

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
        } )
            .doOnNext(new Action1<EntitySet>() {
                @Override
                public void call(EntitySet entitySet) {
                    loadMeter.mark();
                }
            })
            .doOnCompleted( new Action0() {
                @Override
                public void call() {
                    timer.stop();
                }
            } );
    }


    @Override
    public Observable<Id> getIdField(final String type,  final Field field ) {
        final List<Field> fields = Collections.singletonList( field );
        return rx.Observable.from( fields ).map( new Func1<Field, Id>() {
            @Override
            public Id call( Field field ) {
                try {
                    final UniqueValueSet set = uniqueValueSerializationStrategy.load( applicationScope, type, fields );
                    final UniqueValue value = set.getValue( field.getName() );
                    return value == null ? null : value.getEntityId();
                }
                catch ( ConnectionException e ) {
                    logger.error( "Failed to getIdField", e );
                    throw new RuntimeException( e );
                }
            }
        } );
    }


    /**
     * Retrieves all entities that correspond to each field given in the Collection.
     * @param fields
     * @return
     */
    @Override
    public Observable<FieldSet> getEntitiesFromFields(final String type, final Collection<Field> fields ) {
        return rx.Observable.just(fields).map( new Func1<Collection<Field>, FieldSet>() {
            @Override
            public FieldSet call( Collection<Field> fields ) {
                try {

                    final UUID startTime = UUIDGenerator.newTimeUUID();

                    //Get back set of unique values that correspond to collection of fields
                    UniqueValueSet set = uniqueValueSerializationStrategy.load( applicationScope,type,  fields );

                    //Short circut if we don't have any uniqueValues from the given fields.
                    if(!set.iterator().hasNext()){
                        return new MutableFieldSet( 0 );
                    }


                    //loop through each field, and construct an entity load
                    List<Id> entityIds = new ArrayList<>(fields.size());
                    List<UniqueValue> uniqueValues = new ArrayList<>(fields.size());

                    for(final Field expectedField: fields) {

                        UniqueValue value = set.getValue(expectedField.getName());

                        if(value ==null){
                            logger.debug( "Field does not correspond to a unique value" );
                        }

                        entityIds.add(value.getEntityId());
                        uniqueValues.add(value);
                    }

                    //Load a entity for each entityId we retrieved.
                    final EntitySet entitySet = entitySerializationStrategy.load(applicationScope, entityIds, startTime);

                    //now loop through and ensure the entities are there.
                    final MutationBatch deleteBatch = keyspace.prepareMutationBatch();

                    final MutableFieldSet response = new MutableFieldSet(fields.size());

                    for(final UniqueValue expectedUnique: uniqueValues) {
                        final MvccEntity entity = entitySet.getEntity(expectedUnique.getEntityId());

                        //bad unique value, delete this, it's inconsistent
                        if(entity == null || !entity.getEntity().isPresent()){
                            final MutationBatch valueDelete = uniqueValueSerializationStrategy.delete(applicationScope, expectedUnique);
                            deleteBatch.mergeShallow(valueDelete);
                            continue;
                        }


                        //else add it to our result set
                        response.addEntity(expectedUnique.getField(),entity);

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
            }
        } );
    }




    // fire the stages
    public Observable<CollectionIoEvent<MvccEntity>> stageRunner( CollectionIoEvent<Entity> writeData,
                                                                  WriteStart writeState ) {

        return Observable.just( writeData ).map( writeState ).doOnNext( new Action1<CollectionIoEvent<MvccEntity>>() {

                    @Override
                    public void call( final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent ) {

                        Observable<CollectionIoEvent<MvccEntity>> unique =
                                Observable.just( mvccEntityCollectionIoEvent ).subscribeOn( Schedulers.io() )
                                          .doOnNext( writeVerifyUnique );


                        // optimistic verification
                        Observable<CollectionIoEvent<MvccEntity>> optimistic =
                                Observable.just( mvccEntityCollectionIoEvent ).subscribeOn( Schedulers.io() )
                                          .doOnNext( writeOptimisticVerify );


                        //wait for both to finish
                        Observable.merge( unique, optimistic ).toBlocking().last();
                    }
                } );
    }


    @Override
    public Observable<VersionSet> getLatestVersion( final Collection<Id> entityIds ) {

        final Timer.Context timer = getLatestTimer.time();
        return Observable.create( new Observable.OnSubscribe<VersionSet>() {

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
        } )
            .doOnCompleted( new Action0() {
                @Override
                public void call() {
                    timer.stop();
                }
            } );
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
