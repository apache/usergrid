package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
public class MvccEntityNew implements WriteStage {

    private static final Logger LOG = LoggerFactory.getLogger( MvccEntityNew.class );

    private final MvccLogEntrySerializationStrategy logStrategy;
    private final TimeService timeService;
    private final UUIDService uuidService;



    /** Create a new stage with the current context */
    @Inject
    public MvccEntityNew( final MvccLogEntrySerializationStrategy logStrategy, final TimeService timeService,
                          final UUIDService uuidService ) {
        this.logStrategy = logStrategy;
        this.timeService = timeService;
        this.uuidService = uuidService;
    }


    /**
     * Create the entity Id  and inject it, as well as set the timestamp versions
     * @param writeContext The context of the current write operation
     */
    @Override
    public void performStage( final WriteContext writeContext) {

        final Entity entity = writeContext.getMessage(Entity.class);

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );


        final UUID entityId = uuidService.newTimeUUID();
        final UUID version = entityId;
        final long created = timeService.getTime();


        try {
            FieldUtils.writeDeclaredField( entity, "uuid", entityId );
        }
        catch ( Throwable t ) {
            LOG.error( "Unable to set uuid.  See nested exception", t );
            throw new CollectionRuntimeException( "Unable to set uuid.  See nested exception", t );
        }

        entity.setVersion( version );
        entity.setCreated( created );
        entity.setUpdated( created );

        final CollectionContext collectionContext = writeContext.getCollectionContext();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.ACTIVE );

        MutationBatch write = logStrategy.write(collectionContext,  startEntry );

        ListenableFuture<OperationResult<Void>> future;

        try {
            future = write.executeAsync();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }

        //create the mvcc entity for the next stage
        MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, entity );

        writeContext.setMessage( nextStage );


        //set the next stage to invoke on return
        WriteContextCallback.createCallback( future, writeContext );
    }


}
