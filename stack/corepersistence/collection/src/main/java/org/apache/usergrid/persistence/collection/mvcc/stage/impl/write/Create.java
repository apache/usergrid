package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.stage.EventStage;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.collection.util.Verify;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This is the first stage and should be invoked immediately when a new entity create is started. No UUIDs should be
 * present, and this should set the entityId, version, created, and updated dates
 */
@Singleton
public class Create implements EventStage<EventCreate> {

    private static final Logger LOG = LoggerFactory.getLogger( Create.class );


    private final CollectionEventBus eventBus;
    private final TimeService timeService;
    private final UUIDService uuidService;


    @Inject
    public Create( final CollectionEventBus eventBus, final TimeService timeService, final UUIDService uuidService ) {

        Preconditions.checkNotNull( eventBus, "eventBus is required" );
        Preconditions.checkNotNull( timeService, "timeService is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );


        this.eventBus = eventBus;
        this.timeService = timeService;
        this.uuidService = uuidService;
        this.eventBus.register( this );
    }


    @Override
    @Subscribe
    public void performStage( final EventCreate event ) {
        Preconditions.checkNotNull(event,  "event is required" );

        final Entity entity = event.getData();

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        Verify.isNull( entity.getUuid(), "A new entity should not have an id set.  This is an update operation" );


        final UUID entityId = uuidService.newTimeUUID();
        final UUID version = entityId;
        final long created = timeService.getTime();


        try {
            FieldUtils.writeDeclaredField( entity, "uuid", entityId, true );
        }
        catch ( Throwable t ) {
            LOG.error( "Unable to set uuid.  See nested exception", t );
            throw new CollectionRuntimeException( "Unable to set uuid.  See nested exception", t );
        }

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( created );

        eventBus.post( new EventStart( event.getCollectionContext(), entity, event.getResult() ) );
    }
}
