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
import org.apache.usergrid.persistence.model.entity.Entity;

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
public class StartWrite implements Stage {

    private static final Logger LOG = LoggerFactory.getLogger( StartWrite.class );

    private final MvccLogEntrySerializationStrategy logStrategy;


    /** Create a new stage with the current context */
    @Inject
    public StartWrite( final MvccLogEntrySerializationStrategy logStrategy ) {
        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );


        this.logStrategy = logStrategy;
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
        final UUID version = entity.getVersion();

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
        final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, entity );

        executionContext.setMessage( nextStage );
        executionContext.proceed();
    }
}
