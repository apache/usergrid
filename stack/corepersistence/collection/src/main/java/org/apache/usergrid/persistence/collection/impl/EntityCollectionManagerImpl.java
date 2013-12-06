package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.DeleteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.load.EventLoad;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.EventStart;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * Simple implementation.  Should perform
 *
 * @author tnine
 */
public class EntityCollectionManagerImpl implements EntityCollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionManagerImpl.class );

    private final EntityCollection context;
    private final CollectionEventBus eventBus;
    private final UUIDService uuidService;


    @Inject
    public EntityCollectionManagerImpl( final CollectionEventBus eventBus, final UUIDService uuidService,
                                        @Assisted final EntityCollection context ) {


        Preconditions.checkNotNull( eventBus, "eventBus must be defined" );
        Preconditions.checkNotNull( uuidService, "uuidService must be defined" );
        Preconditions.checkNotNull( context, "context must be defined" );
        this.eventBus = eventBus;
        this.uuidService = uuidService;
        this.context = context;
    }


    @Override
    public Entity write( final Entity entity ) {
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


        // Create a new context for the write
        Result result = new Result();

        //fire the start event
        eventBus.post( new EventStart( context, entity, result ) );


        MvccEntity completed = result.getLast( MvccEntity.class );

        return completed.getEntity().get();
    }


    @Override
    public void delete( final Id entityId ) {


        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id is required in this stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity type is required in this stage" );


        eventBus.post( new DeleteStart( context, entityId, null ) );
    }


    @Override
    public Entity load( final Id entityId ) {

        Preconditions.checkNotNull( entityId, "Entity id required in the load stage" );
        Preconditions.checkNotNull( entityId.getUuid(), "Entity id uuid required in the load stage" );
        Preconditions.checkNotNull( entityId.getType(), "Entity id type required in the load stage" );

        Result result = new Result();

        eventBus.post( new EventLoad( context, entityId, result ) );

        return result.getLast( Entity.class );
    }
}
