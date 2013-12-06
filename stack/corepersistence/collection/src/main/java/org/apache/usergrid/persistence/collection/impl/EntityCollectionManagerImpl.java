package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.IoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.Delete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.StartDelete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Commit;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.StartWrite;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Verify;
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
 * Simple implementation.  Should perform
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );

    private final EntityCollection context;
    private final UUIDService uuidService;

    //start stages
    private final StartWrite startWrite;
    private final Verify verifyWrite;
    private final Commit commit;

    //load stages
    private final Load load;


    //delete stages
    private final StartDelete deleteStart;
    private final Delete deleteCommit;


    @Inject
    public EntityCollectionManagerImpl( final UUIDService uuidService, final StartWrite startWrite,
                                        final Verify verifyWrite, final Commit commit,


                                        final Load load, final StartDelete deleteStart, final Delete deleteCommit,
                                        @Assisted final EntityCollection context ) {

        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        Preconditions.checkNotNull( context, "context must be defined" );
        this.startWrite = startWrite;
        this.verifyWrite = verifyWrite;
        this.commit = commit;
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
        //TODO use our own executor here
        return startWrite.call( new IoEvent<Entity>( context, entity ) ).mapMany( verifyWrite ).mapMany( commit );
    }


    @Override
    public Subscription delete( final Id entityId ) {


        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );



       return deleteStart.call( new IoEvent<Id>( context, entityId ) ).subscribe( deleteCommit );

        //        eventBus.post( new DeleteStart( context, entityId, null ) );
    }


    @Override
    public Observable<Entity> load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in the load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in the load stage" );


        return load.call( new IoEvent<Id>( context, entityId ) );
    }
}
