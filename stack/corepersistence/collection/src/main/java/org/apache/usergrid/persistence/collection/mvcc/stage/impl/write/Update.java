package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.stage.EventStage;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This stage performs the initial commit log and write of an entity.  It assumes the entity id and created has already
 * been set correctly
 */
@Singleton
public class Update implements EventStage<EventUpdate> {

    private static final Logger LOG = LoggerFactory.getLogger( Update.class );

    private final TimeService timeService;
    private final UUIDService uuidService;
    private final CollectionEventBus eventBus;


    @Inject
    public Update( final CollectionEventBus eventBus, final TimeService timeService, final UUIDService uuidService ) {
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
    public void performStage( final EventUpdate event ) {

        final Entity entity = event.getData();

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final UUID entityId = entity.getUuid();

        Preconditions.checkNotNull( entityId, "The entity id is required to be set for an update operation" );


        final UUID version = uuidService.newTimeUUID();
        final long updated = timeService.getTime();


        entity.setVersion( version );
        entity.setUpdated( updated );

        //fire the start event
        eventBus.post(new EventStart( event.getCollectionContext(), entity, event.getResult()) );
    }
}
