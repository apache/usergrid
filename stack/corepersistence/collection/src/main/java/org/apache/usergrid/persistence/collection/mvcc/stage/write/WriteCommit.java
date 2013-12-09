package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.Scope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
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


/** This phase should invoke any finalization, and mark the entity as committed in the data store before returning */
@Singleton
public class WriteCommit implements Func1<IoEvent<MvccEntity>, Observable<Entity>> {


    private static final Logger LOG = LoggerFactory.getLogger( WriteCommit.class );

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;


    @Inject
    public WriteCommit( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                        final MvccEntitySerializationStrategy entitySerializationStrategy ) {
        Preconditions.checkNotNull( logEntrySerializationStrategy, "logEntrySerializationStrategy is required" );
        Preconditions.checkNotNull( entitySerializationStrategy, "entitySerializationStrategy is required" );


        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
    }


    @Override
    public Observable<Entity> call( final IoEvent<MvccEntity> ioEvent ) {

        final MvccEntity entity = ioEvent.getEvent();


        EntityUtils.verifyMvccEntityWithEntity( entity );

        final Id entityId = entity.getId();
        final UUID version = entity.getVersion();

        final Scope scope = ioEvent.getEntityCollection();


        final MvccLogEntry startEntry = new MvccLogEntryImpl( entityId, version,
                org.apache.usergrid.persistence.collection.mvcc.entity.Stage.COMMITTED );

        MutationBatch logMutation = logEntrySerializationStrategy.write( scope, startEntry );

        //now get our actual insert into the entity data
        MutationBatch entityMutation = entitySerializationStrategy.write( scope, entity );

        //merge the 2 into 1 mutation
        logMutation.mergeShallow( entityMutation );


        try {
            //TODO Async execution
            logMutation.execute();
        }
        catch ( ConnectionException e ) {
            LOG.error( "Failed to execute write asynchronously ", e );
            throw new CollectionRuntimeException( "Failed to execute write asynchronously ", e );
        }

        return Observable.from( entity.getEntity().get() );
    }
}
