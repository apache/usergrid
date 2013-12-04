package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.Stage;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
@Singleton
public class StartDelete implements Stage {

    private static final Logger LOG = LoggerFactory.getLogger( StartDelete.class );

    private final MvccLogEntrySerializationStrategy logStrategy;
    private final UUIDService uuidService;


    /** Create a new stage with the current context */
    @Inject
    public StartDelete( final MvccLogEntrySerializationStrategy logStrategy, final UUIDService uuidService ) {

        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );
        Preconditions.checkNotNull( uuidService, "uuidService is required" );


        this.logStrategy = logStrategy;
        this.uuidService = uuidService;
    }


    /**
     * Create the entity Id  and inject it, as well as set the timestamp versions
     *
     * @param executionContext The context of the current write operation
     */
    @Override
    public void performStage( final ExecutionContext executionContext ) {

        final UUID entityId = executionContext.getMessage( UUID.class );


        final UUID version = uuidService.newTimeUUID();

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( version, "Entity version is required in this stage" );



        final CollectionContext collectionContext = executionContext.getCollectionContext();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, org.apache.usergrid.persistence
                .collection.mvcc.entity.Stage.ACTIVE );

        MutationBatch write = logStrategy.write( collectionContext, startEntry );


        try {
            write.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }


        //create the mvcc entity for the next stage
        final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, Optional.<Entity>absent() );

        executionContext.setMessage( nextStage );
        executionContext.proceed();
    }
}
