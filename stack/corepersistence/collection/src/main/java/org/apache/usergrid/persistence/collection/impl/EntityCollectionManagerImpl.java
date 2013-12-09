package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.Scope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.DeleteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.delete.DeleteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteCommit;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteVerify;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;
import rx.Subscription;


/**
 * Simple implementation.  Should perform  writes, delete and load.
 *
 * TODO T.N. maybe refactor the stage operations into their own classes for clarity and organization?
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );

    private final Scope context;
    private final UUIDService uuidService;

    //start stages
    private final WriteStart writeStart;
    private final WriteVerify writeVerifyWrite;
    private final WriteCommit writeCommit;

    //load stages
    private final Load load;


    //delete stages
    private final DeleteStart deleteStart;
    private final DeleteCommit deleteCommit;


    @Inject
    public EntityCollectionManagerImpl( final UUIDService uuidService, final WriteStart writeStart,
                                        final WriteVerify writeVerifyWrite, final WriteCommit writeCommit,


                                        final Load load, final DeleteStart deleteStart, final DeleteCommit deleteCommit,
                                        @Assisted final Scope context ) {

        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        Preconditions.checkNotNull( context, "context must be defined" );
        this.writeStart = writeStart;
        this.writeVerifyWrite = writeVerifyWrite;
        this.writeCommit = writeCommit;
        this.load = load;
        this.deleteStart = deleteStart;
        this.deleteCommit = deleteCommit;


        this.uuidService = uuidService;
        this.context = context;
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


        //fire the stages
        //TODO use our own scheduler to help with multitennancy here
        return writeStart.call( new IoEvent<Entity>( context, entity ) ).mapMany( writeVerifyWrite )
                         .mapMany( writeCommit );
    }


    @Override
    public Subscription delete( final Id entityId ) {


        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );


        //TODO use our own scheduler to help with multitennancy here
        return deleteStart.call( new IoEvent<Id>( context, entityId ) ).subscribe( deleteCommit );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in the load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in the load stage" );

        //TODO use our own scheduler to help with multitennancy here
        return load.call( new IoEvent<Id>( context, entityId ) );
    }
}
