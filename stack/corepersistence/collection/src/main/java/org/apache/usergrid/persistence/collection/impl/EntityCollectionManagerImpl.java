package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
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
import rx.Scheduler;


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
    private final Scheduler scheduler;


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
    public EntityCollectionManagerImpl( final UUIDService uuidService,  final Scheduler scheduler, final WriteStart writeStart,
                                        final WriteUniqueVerify writeVerifyUnique,
                                        final WriteOptimisticVerify writeOptimisticVerify,
                                        final WriteCommit writeCommit, final Load load, final DeleteStart deleteStart,
                                        final DeleteCommit deleteCommit,
                                        @Assisted final CollectionScope collectionScope ) {


        Preconditions.checkNotNull( scheduler, "scheduler is required" );
        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        ValidationUtils.validateCollectionScope( collectionScope );

        this.scheduler = scheduler;


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
        Observable<CollectionIoEvent<MvccEntity>> observable =  Observable.just( new CollectionIoEvent<Entity>( collectionScope, entity ) ).subscribeOn(
                scheduler ).map( writeStart );


        //execute all validation stages concurrently.  Needs refactored when this is done.  https://github.com/Netflix/RxJava/issues/627
        observable = Concurrent.concurrent(scheduler, observable, writeVerifyUnique, writeOptimisticVerify);

        //return the commit result.
        return observable.map( writeCommit );
    }


    @Override
    public Observable<Void> delete( final Id entityId ) {


        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );


        //TODO use our own scheduler to help with multitenancy here
        return Observable.just( new CollectionIoEvent<Id>( collectionScope, entityId ) ).subscribeOn(
                scheduler) .map( deleteStart ).map( deleteCommit );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in the load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in the load stage" );

        //TODO use our own scheduler to help with multitenancy here
        return Observable.just( new CollectionIoEvent<Id>( collectionScope, entityId ) ).subscribeOn(
                scheduler ).map( load );
    }
}
