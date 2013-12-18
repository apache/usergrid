package org.apache.usergrid.persistence.collection.mvcc.stage.delete;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.util.functions.Func1;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
@Singleton
public class DeleteStart implements Func1<CollectionIoEvent<Id>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( DeleteStart.class );


    private final MvccLogEntrySerializationStrategy logStrategy;
    private final UUIDService uuidService;


    /**
     * Create a new stage with the current context
     */
    @Inject
    public DeleteStart( final MvccLogEntrySerializationStrategy logStrategy, final UUIDService uuidService ) {
        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );

        this.logStrategy = logStrategy;
        this.uuidService = uuidService;
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<Id> entityIoEvent ) {
        final Id entityId = entityIoEvent.getEvent();

        ValidationUtils.verifyIdentity( entityId );

        final UUID version = uuidService.newTimeUUID();


        final CollectionScope collectionScope = entityIoEvent.getEntityCollection();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.ACTIVE );

        MutationBatch write = logStrategy.write( collectionScope, startEntry );


        try {
            write.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }


        //create the mvcc entity for the next stage
        final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, Optional.<Entity>absent() );


        return new CollectionIoEvent<MvccEntity>( collectionScope, nextStage );
    }
}
