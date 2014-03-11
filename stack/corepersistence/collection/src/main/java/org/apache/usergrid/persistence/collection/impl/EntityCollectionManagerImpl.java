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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.MarkStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.FuncN;


/**
 * Simple implementation.  Should perform  writes, delete and load.
 *
 * TODO: maybe refactor the stage operations into their own classes for clarity and organization?
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger(EntityCollectionManagerImpl.class);

    private final CollectionScope collectionScope;
    private final UUIDService uuidService;
    private final Scheduler scheduler;


    //start stages
    private final WriteStart writeStart;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;

    //load stages
    private final Load load;


    //delete stages
    private final MarkStart markStart;
    private final MarkCommit markCommit;


    @Inject
    public EntityCollectionManagerImpl( 
        final UUIDService uuidService, 
        final WriteStart writeStart,
        final Scheduler scheduler, 
        final WriteUniqueVerify writeVerifyUnique,
        final WriteOptimisticVerify writeOptimisticVerify,
        final WriteCommit writeCommit, 
        final Load load, 
        final MarkStart markStart,
        final MarkCommit markCommit,
        @Assisted final CollectionScope collectionScope ) {

        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        ValidationUtils.validateCollectionScope( collectionScope );

        this.writeStart = writeStart;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.load = load;
        this.markStart = markStart;
        this.markCommit = markCommit;

        this.uuidService = uuidService;
        this.scheduler = scheduler;
        this.collectionScope = collectionScope;
    }


    @Override
    public Observable<Entity> write( final Entity entity ) {

        //do our input validation
        Preconditions.checkNotNull( entity, 
            "Entity is required in the new stage of the mvcc write" );

        final Id entityId = entity.getId();

        Preconditions.checkNotNull( entityId, 
            "The entity id is required to be set for an update operation" );

        Preconditions.checkNotNull( entityId.getUuid(), 
            "The entity id uuid is required to be set for an update operation" );

        Preconditions.checkNotNull( entityId.getType(), 
            "The entity id type required to be set for an update operation" );


        final UUID version = uuidService.newTimeUUID();

        EntityUtils.setVersion( entity, version );


        // fire the stages
        // TODO use our own scheduler to help with multitenancy here.
        // TODO writeOptimisticVerify and writeVerifyUnique should be concurrent to reduce wait time
        // these 3 lines could be done in a single line, but they are on multiple lines for clarity

        // create our observable and start the write
        CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>( collectionScope, entity );

        Observable<CollectionIoEvent<MvccEntity>> observable =
            Observable.from( writeData ).subscribeOn( scheduler ).map( writeStart ).flatMap(
                new Func1<CollectionIoEvent<MvccEntity>, Observable<CollectionIoEvent<MvccEntity>>>() {

                    @Override
                    public Observable<CollectionIoEvent<MvccEntity>> call(
                            final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent ) {

                        // do the unique and optimistic steps in parallel

                        // unique function.  Since there can be more than 1 unique value in this 
                        // entity the unique verify step itself is multiple parallel executions.
                        // This is why we use "flatMap" instead of "map", which allows the
                        // WriteVerifyUnique stage to execute multiple verification steps in 
                        // parallel and zip the results


                        Observable<CollectionIoEvent<MvccEntity>> unique =
                            Observable.from( mvccEntityCollectionIoEvent ).subscribeOn( scheduler )
                                .flatMap( writeVerifyUnique);


                        // optimistic verification
                        Observable<CollectionIoEvent<MvccEntity>> optimistic =
                            Observable.from( mvccEntityCollectionIoEvent ).subscribeOn( scheduler )
                                .map( writeOptimisticVerify );


                        // zip the results
                        // TODO: Should the zip only return errors here, and if errors are present, 
                        // we throw during the zip phase?  I couldn't find "

                        return Observable.zip( unique, optimistic, new Func2<CollectionIoEvent<MvccEntity>,
                                CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>>() {
                            @Override
                            public CollectionIoEvent<MvccEntity> call(
                                final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent,
                                final CollectionIoEvent<MvccEntity> mvccEntityCollectionIoEvent2 ) {

                            return mvccEntityCollectionIoEvent;
                           }
                       } );
                    }
                } );


        // execute all validation stages concurrently.  Needs refactored when this is done.  
        // https://github.com/Netflix/RxJava/issues/627
        // observable = Concurrent.concurrent( observable, scheduler, new WaitZip(), 
        //                  writeVerifyUnique, writeOptimisticVerify );

        //return the commit result.
        return observable.map( writeCommit );
    }


    @Override
    public Observable<Void> delete( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );

        return Observable.from( new CollectionIoEvent<Id>( collectionScope, entityId ) )
            .subscribeOn( scheduler ).map( markStart ).map( markCommit );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in load stage");
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in load stage");

        return Observable.from( new CollectionIoEvent<Id>( collectionScope, entityId ) )
            .subscribeOn( scheduler ).map( load );
    }


    /**
     * Class that validates all results are equal then proceeds
     */
    private static class WaitZip<R> implements FuncN<R> {


        private WaitZip() {
        }


        @Override
        public R call( final Object... args ) {

            for ( int i = 0; i < args.length - 1; i++ ) {
                assert args[i] == args[i + 1];
            }

            return ( R ) args[0];
        }
    }
}
