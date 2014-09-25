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


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.guice.Write;
import org.apache.usergrid.persistence.collection.guice.WriteUpdate;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.RollbackAction;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Simple implementation.  Should perform  writes, delete and load.
 *
 * TODO: maybe refactor the stage operations into their own classes for clarity and organization?
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger log = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );

    private final CollectionScope collectionScope;
    private final UUIDService uuidService;


    //start stages
    private final WriteStart writeStart;
    private final WriteStart writeUpdate;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;
    private final RollbackAction rollback;

    //load stages
    private final Load load;


    //delete stages
    private final MarkStart markStart;
    private final MarkCommit markCommit;

    private final TaskExecutor taskExecutor;

    @Inject
    public EntityCollectionManagerImpl( final UUIDService uuidService, @Write final WriteStart writeStart,
                                        @WriteUpdate final WriteStart writeUpdate,
                                        final WriteUniqueVerify writeVerifyUnique,
                                        final WriteOptimisticVerify writeOptimisticVerify,
                                        final WriteCommit writeCommit, final RollbackAction rollback, final Load load,
                                        final MarkStart markStart, final MarkCommit markCommit,
                                        final TaskExecutor taskExecutor,
                                        @Assisted final CollectionScope collectionScope) {

        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );

        MvccValidationUtils.validateCollectionScope( collectionScope );

        this.writeStart = writeStart;
        this.writeUpdate = writeUpdate;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.rollback = rollback;

        this.load = load;
        this.markStart = markStart;
        this.markCommit = markCommit;

        this.uuidService = uuidService;
        this.collectionScope = collectionScope;
        this.taskExecutor = taskExecutor;
    }


    @Override
    public Observable<Entity> write( final Entity entity ) {

        //do our input validation
        Preconditions.checkNotNull( entity, 
            "Entity is required in the new stage of the mvcc write" );

        final Id entityId = entity.getId();

        ValidationUtils.verifyIdentity( entityId );


        // create our observable and start the write
        CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>( collectionScope, entity );

        Observable<CollectionIoEvent<MvccEntity>> observable = stageRunner( writeData,writeStart );

        // execute all validation stages concurrently.  Needs refactored when this is done.  
        // https://github.com/Netflix/RxJava/issues/627
        // observable = Concurrent.concurrent( observable, Schedulers.io(), new WaitZip(), 
        //                  writeVerifyUnique, writeOptimisticVerify );

        observable.doOnNext( new Action1<CollectionIoEvent<MvccEntity>>() {
            @Override
            public void call( final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent ) {
                //Queue future write here (verify)
            }
        } ).map( writeCommit ).doOnNext( new Action1<Entity>() {
            @Override
            public void call( final Entity entity ) {
                //fork background processing here (start)

                //post-processing to come later. leave it empty for now.
            }
        } ).doOnError( rollback );


        // return the commit result.
        return observable.map( writeCommit ).doOnError( rollback );
    }


    @Override
    public Observable<Void> delete( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );

        return Observable.from( new CollectionIoEvent<Id>( collectionScope, entityId ) )
                         .map( markStart ).map( markCommit );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in load stage" );

        return Observable.from( new CollectionIoEvent<Id>( collectionScope, entityId ) )
                         .map( load );
    }

    @Override
    public Observable<Entity> update( final Entity entity ) {

        log.debug( "Starting update process" );

        //do our input validation
        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final Id entityId = entity.getId();


        ValidationUtils.verifyIdentity( entityId );

        // create our observable and start the write
        CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>( collectionScope, entity );


        Observable<CollectionIoEvent<MvccEntity>> observable = stageRunner( writeData,writeUpdate );


        return observable.map( writeCommit ).doOnNext( new Action1<Entity>() {
            @Override
            public void call( final Entity entity ) {
                log.debug( "sending entity to the queue" );

               //we an update, signal the fix

                //TODO T.N Change this to use request collapsing
                Observable.from( new CollectionIoEvent<Id>(collectionScope, entityId ) ).map( load ).subscribeOn( Schedulers.io() ).subscribe();


            }
        } ).doOnError( rollback );
    }

    // fire the stages
    public Observable<CollectionIoEvent<MvccEntity>> stageRunner( CollectionIoEvent<Entity> writeData,WriteStart writeState ) {

        return Observable.from( writeData ).map( writeState ).flatMap(
                new Func1<CollectionIoEvent<MvccEntity>, Observable<CollectionIoEvent<MvccEntity>>>() {

                    @Override
                    public Observable<CollectionIoEvent<MvccEntity>> call(
                            final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent ) {

                        Observable<CollectionIoEvent<MvccEntity>> unique =
                                Observable.from( mvccEntityCollectionIoEvent ).subscribeOn( Schedulers.io() )
                                          .flatMap( writeVerifyUnique );


                        // optimistic verification
                        Observable<CollectionIoEvent<MvccEntity>> optimistic =
                                Observable.from( mvccEntityCollectionIoEvent ).subscribeOn( Schedulers.io() )
                                          .map( writeOptimisticVerify );


                        return Observable.merge( unique, optimistic).last();
                    }
                } );
    }




}
