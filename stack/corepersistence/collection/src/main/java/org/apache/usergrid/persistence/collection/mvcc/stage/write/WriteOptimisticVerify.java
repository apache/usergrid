package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import static antlr.Version.version;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.List;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccLogEntryImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.util.functions.Func1;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    private static final Logger LOG = LoggerFactory.getLogger( WriteOptimisticVerify.class );

    private final MvccLogEntrySerializationStrategy logEntryStrat;

    @Inject
    public WriteOptimisticVerify( WriteFig writeFig,
            MvccLogEntrySerializationStrategy logEntryStrat ) {

        this.logEntryStrat = logEntryStrat;

    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> mvccEntityIoEvent ) {
        ValidationUtils.verifyMvccEntityWithEntity( mvccEntityIoEvent.getEvent() );

        // If the version was included on the entity write operation (delete or write) we need
        // to read back the entity log, and ensure that our "new" version is the only version
        // entry since the last commit.
        //
        // If not, fail fast, signal to the user their entity is "stale".

        MvccEntity entity = mvccEntityIoEvent.getEvent();
        CollectionScope collectionScope = mvccEntityIoEvent.getEntityCollection();

        try {
            List<MvccLogEntry> versions = logEntryStrat.load( collectionScope, 
                    entity.getId(), entity.getVersion(), 2 );

            // previous log entry must be committed, otherwise somebody is already writing
            if ( versions.size() > 1 
                    && versions.get(1).getStage().ordinal() < Stage.COMMITTED.ordinal() ) {
            
                // we're not the first writer, rollback and throw-up
                final MvccLogEntry rollbackEntry = 
                        new MvccLogEntryImpl( entity.getId(), entity.getVersion(), Stage.ROLLBACK);
                logEntryStrat.write( collectionScope, rollbackEntry );
                throw new CollectionRuntimeException("Change conflict, not first writer");
            }


        } catch ( ConnectionException e ) {
            LOG.error( "Error reading entity log", e );
            throw new CollectionRuntimeException( "Error reading entity log", e );
        }

        //no op, just emit the value
        return mvccEntityIoEvent;
    }
}
