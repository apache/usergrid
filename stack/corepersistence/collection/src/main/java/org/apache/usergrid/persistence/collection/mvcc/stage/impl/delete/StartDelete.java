package org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.EventStage;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
@Singleton
public class StartDelete implements EventStage<DeleteStart> {

    private static final Logger LOG = LoggerFactory.getLogger( StartDelete.class );

    private final CollectionEventBus eventBus;
    private final MvccLogEntrySerializationStrategy logStrategy;
    private final UUIDService uuidService;


    /** Create a new stage with the current context */
    @Inject
    public StartDelete( final CollectionEventBus eventBus, final MvccLogEntrySerializationStrategy logStrategy,
                        final UUIDService uuidService ) {
        Preconditions.checkNotNull( eventBus, "eventBus is required" );
        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );


        this.eventBus = eventBus;
        this.logStrategy = logStrategy;
        this.uuidService = uuidService;

        this.eventBus.register( this );
    }


    @Override
    @Subscribe
    public void performStage( final DeleteStart event ) {

        final Id entityId = event.getData();


        final UUID version = uuidService.newTimeUUID();


        final EntityCollection entityCollection = event.getCollectionContext();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.ACTIVE );

        MutationBatch write = logStrategy.write( entityCollection, startEntry );


        try {
            write.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }


        //create the mvcc entity for the next stage
        final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, Optional.<Entity>absent() );

        eventBus.post( new DeleteCommit( entityCollection, nextStage, event.getResult() ) );
    }
}
