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


import org.apache.usergrid.persistence.collection.EntityVersionCleanupFactory;
import java.net.ConnectException;
import java.util.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.VersionSet;
import org.apache.usergrid.persistence.collection.guice.CollectionTaskExecutor;
import org.apache.usergrid.persistence.collection.guice.Write;
import org.apache.usergrid.persistence.collection.guice.WriteUpdate;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Simple implementation.  Should perform  writes, delete and load.
 * <p/>
 * TODO: maybe refactor the stage operations into their own classes for clarity and organization?
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger log = LoggerFactory.getLogger(EntityCollectionManagerImpl.class);

    private final CollectionScope collectionScope;
    private final UUIDService uuidService;


    //start stages
    private final WriteStart writeStart;
    private final WriteStart writeUpdate;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;
    private final RollbackAction rollback;


    //delete stages
    private final MarkStart markStart;
    private final MarkCommit markCommit;

    private final TaskExecutor taskExecutor;
    private EntityVersionCleanupFactory entityVersionCleanupFactory;
    private final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;


    @Inject
    public EntityCollectionManagerImpl( final UUIDService uuidService, @Write final WriteStart writeStart,
                                        @WriteUpdate final WriteStart writeUpdate,
                                        final WriteUniqueVerify writeVerifyUnique,
                                        final WriteOptimisticVerify writeOptimisticVerify,
                                        final WriteCommit writeCommit, final RollbackAction rollback,
                                        final MarkStart markStart, final MarkCommit markCommit,
                                        final EntityVersionCleanupFactory entityVersionCleanupFactory,
                                       final MvccEntitySerializationStrategy entitySerializationStrategy,
                                       final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                       final MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy,
                                       @CollectionTaskExecutor final TaskExecutor taskExecutor,
                                       @Assisted final CollectionScope collectionScope
    ) {
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;


        Preconditions.checkNotNull(uuidService, "uuidService must be defined");

        MvccValidationUtils.validateCollectionScope(collectionScope);

        this.writeStart = writeStart;
        this.writeUpdate = writeUpdate;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.rollback = rollback;


        this.markStart = markStart;
        this.markCommit = markCommit;

        this.uuidService = uuidService;
        this.collectionScope = collectionScope;
        this.taskExecutor = taskExecutor;
        this.entityVersionCleanupFactory = entityVersionCleanupFactory;
        this.mvccLogEntrySerializationStrategy = mvccLogEntrySerializationStrategy;
    }


    @Override
    public Observable<Entity> write(final Entity entity) {

        //do our input validation
        Preconditions.checkNotNull(entity, "Entity is required in the new stage of the mvcc write");

        final Id entityId = entity.getId();

        ValidationUtils.verifyIdentity(entityId);


        // create our observable and start the write
        final CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>(collectionScope, entity);

        Observable<CollectionIoEvent<MvccEntity>> observable = stageRunner(writeData, writeStart);

        // execute all validation stages concurrently.  Needs refactored when this is done.  
        // https://github.com/Netflix/RxJava/issues/627
        // observable = Concurrent.concurrent( observable, Schedulers.io(), new WaitZip(), 
        //                  writeVerifyUnique, writeOptimisticVerify );

        observable.map(writeCommit).doOnNext(new Action1<Entity>() {
            @Override
            public void call(final Entity entity) {
                //TODO fire a task here
                taskExecutor.submit(entityVersionCleanupFactory.getTask(collectionScope, entityId,entity.getVersion()));
                //post-processing to come later. leave it empty for now.
            }
        }).doOnError(rollback);


        // return the commit result.
        return observable.map(writeCommit).doOnError(rollback);
    }


    @Override
    public Observable<Void> delete(final Id entityId) {

        Preconditions.checkNotNull(entityId, "Entity id is required in this stage");
        Preconditions.checkNotNull(entityId.getUuid(), "Entity id is required in this stage");
        Preconditions.checkNotNull(entityId.getType(), "Entity type is required in this stage");


        return Observable.from(new CollectionIoEvent<Id>(collectionScope, entityId)).map(markStart)
                .doOnNext(markCommit).map(new Func1<CollectionIoEvent<MvccEntity>, Void>() {
                    @Override
                    public Void call(final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent) {
                        return null;
                    }
                });
    }


    @Override
    public Observable<Entity> load(final Id entityId) {

        Preconditions.checkNotNull(entityId, "Entity id required in the load stage");
        Preconditions.checkNotNull(entityId.getUuid(), "Entity id uuid required in load stage");
        Preconditions.checkNotNull(entityId.getType(), "Entity id type required in load stage");

        return load(Collections.singleton(entityId)).map(new Func1<EntitySet, Entity>() {
            @Override
            public Entity call(final EntitySet entitySet) {
                final MvccEntity entity = entitySet.getEntity(entityId);

                if (entity == null) {
                    return null;
                }

                return entity.getEntity().orNull();
            }
        });
    }


    @Override
    public Observable<EntitySet> load(final Collection<Id> entityIds) {

        Preconditions.checkNotNull(entityIds, "entityIds cannot be null");


        return Observable.create(new Observable.OnSubscribe<EntitySet>() {

            @Override
            public void call(final Subscriber<? super EntitySet> subscriber) {
                try {
                    final EntitySet results =
                            entitySerializationStrategy.load(collectionScope, entityIds, UUIDGenerator.newTimeUUID());

                    subscriber.onNext(results);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public Observable<Id> getIdField(final Field field) {
        final List<Field> fields = Collections.singletonList(field);
        return rx.Observable.from(fields).map(new Func1<Field, Id>() {
            @Override
            public Id call(Field field) {
                try {
                    UniqueValueSet set = uniqueValueSerializationStrategy.load(collectionScope, fields);
                    UniqueValue value = set.getValue(field.getName());
                    Id id = value == null ? null : value.getEntityId();
                    return id;
                } catch (ConnectionException e) {
                    log.error("Failed to getIdField", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public Observable<Entity> update(final Entity entity) {

        log.debug("Starting update process");

        //do our input validation
        Preconditions.checkNotNull(entity, "Entity is required in the new stage of the mvcc write");

        final Id entityId = entity.getId();


        ValidationUtils.verifyIdentity(entityId);

        // create our observable and start the write
        CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>(collectionScope, entity);


        Observable<CollectionIoEvent<MvccEntity>> observable = stageRunner(writeData, writeUpdate);


        return observable.map(writeCommit).doOnNext(new Action1<Entity>() {
            @Override
            public void call(final Entity entity) {
                log.debug("sending entity to the queue");

                //we an update, signal the fix

                //TODO T.N Change this to fire a task
                //                Observable.from( new CollectionIoEvent<Id>(collectionScope,
                // entityId ) ).map( load ).subscribeOn( Schedulers.io() ).subscribe();


            }
        }).doOnError(rollback);
    }


    // fire the stages
    public Observable<CollectionIoEvent<MvccEntity>> stageRunner(CollectionIoEvent<Entity> writeData,
                                                                 WriteStart writeState) {

        return Observable.from(writeData).map(writeState).doOnNext(new Action1<CollectionIoEvent<MvccEntity>>() {

            @Override
            public void call(final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent) {

                Observable<CollectionIoEvent<MvccEntity>> unique =
                        Observable.from(mvccEntityCollectionIoEvent).subscribeOn(Schedulers.io())
                                .doOnNext(writeVerifyUnique);


                // optimistic verification
                Observable<CollectionIoEvent<MvccEntity>> optimistic =
                        Observable.from(mvccEntityCollectionIoEvent).subscribeOn(Schedulers.io())
                                .doOnNext(writeOptimisticVerify);


                //wait for both to finish
                Observable.merge(unique, optimistic).toBlocking().last();

            }
        });
    }


    @Override
    public Observable<VersionSet> getLatestVersion(final Collection<Id> entityIds) {

        return Observable.create(new Observable.OnSubscribe<VersionSet>() {

            @Override
            public void call(final Subscriber<? super VersionSet> subscriber) {
                try {
                    final VersionSet logEntries = mvccLogEntrySerializationStrategy
                        .load(collectionScope, entityIds, UUIDGenerator.newTimeUUID());

                    subscriber.onNext(logEntries);
                    subscriber.onCompleted();

                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
