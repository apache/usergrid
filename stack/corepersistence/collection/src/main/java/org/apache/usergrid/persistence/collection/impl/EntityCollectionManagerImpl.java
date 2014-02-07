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


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.Cassandra;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.hystrix.CassandraCommand;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.DeleteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.DeleteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteOptimisticVerify;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteUniqueVerify;
import org.apache.usergrid.persistence.collection.rx.Concurrent;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.util.functions.FuncN;


/**
 * Simple implementation.  Should perform  writes, delete and load.
 *
 * TODO T.N. maybe refactor the stage operations into their own classes for clarity and organization?
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );

    private final CollectionScope collectionScope;
    private final UUIDService uuidService;


    //start stages
    private final WriteStart writeStart;
    private final WriteUniqueVerify writeVerifyUnique;
    private final WriteOptimisticVerify writeOptimisticVerify;
    private final WriteCommit writeCommit;

    //load stages
    private final Load load;


    //delete stages
    private final DeleteStart deleteStart;
    private final DeleteCommit deleteCommit;


    @Inject
    public EntityCollectionManagerImpl( final UUIDService uuidService, final WriteStart writeStart,
                                        final WriteUniqueVerify writeVerifyUnique,
                                        final WriteOptimisticVerify writeOptimisticVerify,
                                        final WriteCommit writeCommit, final Load load, final DeleteStart deleteStart,
                                        final DeleteCommit deleteCommit,
                                        @Assisted final CollectionScope collectionScope ) {


        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        ValidationUtils.validateCollectionScope( collectionScope );


        this.writeStart = writeStart;
        this.writeVerifyUnique = writeVerifyUnique;
        this.writeOptimisticVerify = writeOptimisticVerify;
        this.writeCommit = writeCommit;
        this.load = load;
        this.deleteStart = deleteStart;
        this.deleteCommit = deleteCommit;


        this.uuidService = uuidService;
        this.collectionScope = collectionScope;
    }


    @Override
    public Observable<Entity> write( final Entity entity ) {
        //do our input validation

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final Id entityId = entity.getId();

        Preconditions.checkNotNull( entityId, "The entity id is required to be set for an update operation" );

        Preconditions
                .checkNotNull( entityId.getUuid(), "The entity id uuid is required to be set for an update operation" );

        Preconditions
                .checkNotNull( entityId.getType(), "The entity id type required to be set for an update operation" );


        final UUID version = uuidService.newTimeUUID();

        EntityUtils.setVersion( entity, version );


        /**
         *fire the stages
         * TODO use our own scheduler to help with multitenancy here.
         * TODO writeOptimisticVerify and writeVerifyUnique should happen concurrently to reduce user wait time
         */

        //these 3 lines could be done in a single line, but they are on multiple lines for clarity

        //create our observable and start the write
        CollectionIoEvent<Entity> writeData = new CollectionIoEvent<Entity>( collectionScope, entity );

        Observable<CollectionIoEvent<MvccEntity>> observable = CassandraCommand.toObservable( writeData ).map( writeStart );


        //execute all validation stages concurrently.  Needs refactored when this is done.  https://github.com/Netflix/RxJava/issues/627
        observable = Concurrent.concurrent(observable, new WaitZip( ), writeVerifyUnique, writeOptimisticVerify);

        //return the commit result.
        return observable.map( writeCommit );
    }


    @Override
    public Observable<Void> delete( final Id entityId ) {


        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );


        return CassandraCommand.toObservable( new CollectionIoEvent<Id>( collectionScope, entityId ) ).map( deleteStart ).map( deleteCommit );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in the load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in the load stage" );

        return CassandraCommand.toObservable( new CollectionIoEvent<Id>( collectionScope, entityId ) ).map( load );
    }


    /**
     * Class that validates all results are equal then proceeds
     * @param <R>
     */
    private static class WaitZip<R> implements FuncN<R>{



        private WaitZip() {
        }


        @Override
        public R call( final Object... args ) {

            for(int i = 0; i < args.length-1; i ++){
                assert args[i] == args[i+1];
            }

            return ( R ) args[0];
        }
    }
}
