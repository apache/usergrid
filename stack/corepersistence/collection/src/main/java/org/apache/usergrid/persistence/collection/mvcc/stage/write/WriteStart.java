package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.util.functions.Func1;


/**
 * This is the first stage and should be invoked immediately when a write is started.  It should persist the start of a
 * new write in the data store for a checkpoint and recovery
 */
@Singleton
public class WriteStart implements Func1<IoEvent<Entity>, Observable<IoEvent<MvccEntity>>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteStart.class );

    private final MvccLogEntrySerializationStrategy logStrategy;


    /** Create a new stage with the current context */
    @Inject
    public WriteStart( final MvccLogEntrySerializationStrategy logStrategy ) {
        Preconditions.checkNotNull( logStrategy, "logStrategy is required" );

        this.logStrategy = logStrategy;
    }


    @Override
    public Observable<IoEvent<MvccEntity>> call( final IoEvent<Entity> ioEvent ) {
        {
            final Entity entity = ioEvent.getEvent();
            final EntityCollection entityCollection = ioEvent.getEntityCollection();


            EntityUtils.verifyEntityWrite( entity );

            final Id entityId = entity.getId();
            final UUID version = entity.getVersion();

            final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version,
                    org.apache.usergrid.persistence.collection.mvcc.entity.Stage.ACTIVE );

            MutationBatch write = logStrategy.write( entityCollection, startEntry );


            try {
                write.execute();
            }
            catch ( ConnectionException e ) {
                LOG.error( "Failed to execute write asynchronously ", e );
                throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
            }


            //create the mvcc entity for the next stage
            final MvccEntityImpl nextStage = new MvccEntityImpl( entityId, version, entity );

            return Observable.from( new IoEvent<MvccEntity>( entityCollection, nextStage ) );
        }
    }
}
