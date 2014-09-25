package org.apache.usergrid.persistence.collection.impl;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.serialization.impl.VersionIterator;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Cleans up previous versions from the specified version. Note that this means the version passed in the io event is
 * retained, the range is exclusive.
 */
class EntityVersionCleanupTask extends Task<CollectionIoEvent<EntityVersion>, CollectionIoEvent<EntityVersion>> {

    private static final Logger LOG = LoggerFactory.getLogger( EntityVersionCleanupTask.class );


    private final CollectionIoEvent<EntityVersion> collectionIoEvent;
    private final List<EntityVersionDeleted> listeners;

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;


    private EntityVersionCleanupTask( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                                      final MvccEntitySerializationStrategy entitySerializationStrategy,
                                      final CollectionIoEvent<EntityVersion> collectionIoEvent,
                                      final List<EntityVersionDeleted> listeners ) {
        this.collectionIoEvent = collectionIoEvent;
        this.listeners = listeners;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
    }


    @Override
    public CollectionIoEvent<EntityVersion> getId() {
        return collectionIoEvent;
    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
        LOG.error( "Unable to run update task for event {}", collectionIoEvent, throwable );
    }


    @Override
    public void rejected() {
        //Our task was rejected meaning our queue was full.  We need this operation to run,
        // so we'll run it in our current thread

        try {
            executeTask();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Exception thrown in call task", e );
        }
    }


    @Override
    public CollectionIoEvent<EntityVersion> executeTask() throws Exception {

        final CollectionScope scope = collectionIoEvent.getEntityCollection();
        final Id entityId = collectionIoEvent.getEvent().getId();
        final UUID maxVersion = collectionIoEvent.getEvent().getVersion();


        VersionIterator versionIterator =
                new VersionIterator( logEntrySerializationStrategy, scope, entityId, maxVersion, 1000 );


        UUID currentVersion = null;

        //for every entry, we want to clean it up with listeners

        while ( versionIterator.hasNext() ) {

            currentVersion = versionIterator.next();


            //execute all the listeners
            for ( EntityVersionDeleted listener : listeners ) {
                listener.versionDeleted( scope, entityId, currentVersion );
            }

            //we do multiple invocations on purpose.  Our log is our source of versions, only delete from it
            //after every successful invocation of listeners and entity removal
            entitySerializationStrategy.delete( scope, entityId, currentVersion ).execute();

            logEntrySerializationStrategy.delete( scope, entityId, currentVersion ).execute();
        }


        return collectionIoEvent;
    }
}



