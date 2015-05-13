package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.exception.WriteStartException;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.functions.Func1;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should
 * persist the start of a new write in the data store for a checkpoint and recovery
 */
@Singleton
public class WriteStart implements Func1<CollectionIoEvent<Entity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteStart.class );

    private final MvccLogEntrySerializationStrategy logStrategy;



    /**
     * Create a new stage with the current context and status for entity.
     */

    @Inject
    public WriteStart ( final MvccLogEntrySerializationStrategy logStrategy) {
        this.logStrategy = logStrategy;

    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<Entity> ioEvent ) {
        {
            final Entity entity = ioEvent.getEvent();
            final ApplicationScope applicationScope = ioEvent.getEntityCollection();

            final Id entityId = entity.getId();

            final UUID newVersion = UUIDGenerator.newTimeUUID();

            //TODO update this when merged with George's changes
            final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, newVersion,
                    Stage.ACTIVE, MvccLogEntry.State.COMPLETE);

            MutationBatch write = logStrategy.write( applicationScope, startEntry );

            final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, newVersion, MvccEntity.Status.COMPLETE, entity );
            if(ioEvent.getEvent().hasVersion()) {
                try {
                    write.execute();
                } catch (ConnectionException e) {
                    LOG.error("Failed to execute write ", e);
                    throw new WriteStartException(nextStage, applicationScope,
                        "Failed to execute write ", e);
                } catch (NullPointerException e) {
                    LOG.error("Failed to execute write ", e);
                    throw new WriteStartException(nextStage, applicationScope,
                        "Failed to execute write", e);
                }
            }

            //create the mvcc entity for the next stage
           //TODO: we need to create a complete or partial update here (or sooner)

            return new CollectionIoEvent<>( applicationScope, nextStage );
        }
    }
}
