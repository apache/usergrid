package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionStage;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This stage performs the initial commit log and write of an entity.  It assumes the entity id and created has already
 * been set correctly
 */
@Singleton
public class Update implements ExecutionStage {

    private static final Logger LOG = LoggerFactory.getLogger( Update.class );

    private final TimeService timeService;
    private final UUIDService uuidService;


    @Inject
    public Update( final TimeService timeService, final UUIDService uuidService ) {
        Preconditions.checkNotNull( timeService, "timeService is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );

        this.timeService = timeService;
        this.uuidService = uuidService;
    }


    /**
     * Create the entity Id  and inject it, as well as set the timestamp versions
     *
     * @param executionContext The context of the current write operation
     */
    @Override
    public void performStage( final ExecutionContext executionContext ) {

        final Entity entity = executionContext.getMessage( Entity.class );

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final UUID entityId = entity.getUuid();

        Preconditions.checkNotNull( entityId, "The entity id is required to be set for an update operation" );


        final UUID version = uuidService.newTimeUUID();
        final long updated = timeService.getTime();


        entity.setVersion( version );
        entity.setUpdated( updated );

        executionContext.setMessage( entity );
        executionContext.proceed();
    }
}
