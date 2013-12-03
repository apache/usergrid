package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;



/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
@Singleton
public class Start implements WriteStage {

    private static final Logger LOG = LoggerFactory.getLogger( Start.class );

    private final MvccLogEntrySerializationStrategy logStrategy;
    /**
     * Create a new stage with the current context
     * @param logStrategy
     */
    @Inject
    public Start( final MvccLogEntrySerializationStrategy logStrategy ){
        this.logStrategy = logStrategy;
    }


    @Override
    public void performStage(final WriteContext context, final MvccEntity entity )  {
        final MvccLogEntry startEntry = new MvccLogEntryImpl(entity.getContext(), entity.getUuid(), entity.getVersion(),Stage.ACTIVE);

        MutationBatch write = logStrategy.write( startEntry );

        ListenableFuture<OperationResult<Void>> future;

        try {
            future = write.executeAsync();
        }
        catch ( ConnectionException e ) {
          LOG.error( "Failed to execute write asynchronously ", e );
          throw new RuntimeException( "Failed to execute write asynchronously ", e );
        }

        //todo next stage invocation
        //Futures.addCallback();

    }


}
