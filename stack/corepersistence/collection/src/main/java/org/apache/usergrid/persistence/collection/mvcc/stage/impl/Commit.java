package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/** This phase should invoke any finalization, and mark the entity as committed in the data store before returning */
public class Commit implements WriteStage {


    private static final Logger LOG = LoggerFactory.getLogger( Commit.class );

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;


    @Inject
    public Commit( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                   final MvccEntitySerializationStrategy entitySerializationStrategy ) {
        Preconditions.checkNotNull( logEntrySerializationStrategy, "logEntrySerializationStrategy is required" );
                      Preconditions.checkNotNull( entitySerializationStrategy, "entitySerializationStrategy is required" );


        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
    }


    @Override
    public void performStage( final WriteContext writeContext ) {
        final MvccEntity entity = writeContext.getMessage( MvccEntity.class );

        Preconditions.checkNotNull( entity, "Entity is required in the new stage of the mvcc write" );

        final UUID entityId = entity.getUuid();
        final UUID version = entity.getVersion();

        Preconditions.checkNotNull( entityId, "Entity id is required in this stage" );
        Preconditions.checkNotNull( version, "Entity version is required in this stage" );


        final CollectionContext collectionContext = writeContext.getCollectionContext();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version, Stage.COMMITTED );

        MutationBatch logMutation = logEntrySerializationStrategy.write( collectionContext, startEntry );

        //now get our actual insert into the entity data
        MutationBatch entityMutation = entitySerializationStrategy.write( collectionContext, entity );

        //merge the 2 into 1 mutation
        logMutation.mergeShallow( entityMutation );


        try {
            logMutation.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }

        /**
         * We're done executing.
         */
        writeContext.proceed();

        //TODO connect to post processors via listener
    }
}
