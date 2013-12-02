package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
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

    private final MvccLogEntrySerializationStrategy logStrategy;
    /**
     * Create a new stage with the current context
     * @param logStrategy
     */
    @Inject
    protected Start( final MvccLogEntrySerializationStrategy logStrategy ){
        this.logStrategy = logStrategy;
    }


    @Override
    public MutationBatch performStage( final MvccEntity entity )  {
        final MvccLogEntry startEntry = new MvccLogEntryImpl(entity.getContext(), entity.getUuid(), entity.getVersion(),Stage.ACTIVE);

        MutationBatch write = logStrategy.write( startEntry );

        return write;
    }
}
